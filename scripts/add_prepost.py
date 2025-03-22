import onnx
from onnxruntime_extensions.tools.pre_post_processing import (
    PrePostProcessor,
    create_named_value,
    ConvertImageToBGR,
    ChannelsLastToChannelsFirst,
    Resize,
    LetterBox,
    ImageBytesToFloat,
    Unsqueeze,
    Squeeze,
    Transpose,
    Debug,
    Split,
    SelectBestBoundingBoxesByNMS,
    ScaleNMSBoundingBoxesAndKeyPoints,
    utils
)
from pathlib import Path
import onnx.shape_inference

def _get_model_and_info(input_model_path: Path):
    model = onnx.load(str(input_model_path.resolve(strict=True)))
    model_with_shape_info = onnx.shape_inference.infer_shapes(model)
    model_input_shape = model_with_shape_info.graph.input[0].type.tensor_type.shape
    # 获取模型输出信息
    model_output_shape = model_with_shape_info.graph.output[0].type.tensor_type.shape
    print(f"模型输入形状：{model_input_shape}")
    print(f"模型输出形状：{model_output_shape}")
    # 获取模型输入宽高，假设为最后两个维度
    w_in = model_input_shape.dim[-1].dim_value
    h_in = model_input_shape.dim[-2].dim_value
    assert w_in == 640 and h_in == 640, "模型输入尺寸需要为 640x640"
    return (model, w_in, h_in)

def _update_model(model: onnx.ModelProto, output_model_path: Path, pipeline: PrePostProcessor):
    """
    通过 pipeline 更新模型，将预处理和后处理节点添加到模型中，并保存更新后的模型。
    """
    new_model = pipeline.run(model)
    # 运行 shape inferencing 校验新模型
    _ = onnx.shape_inference.infer_shapes(new_model, strict_mode=True)
    onnx.save_model(new_model, str(output_model_path.resolve()))
    print(f"更新后的模型已保存到 {output_model_path}")

def add_pre_post_processing_for_face(input_model_path: Path, output_model_path: Path):
    """
    为基于 YOLOv8-pose（进一步训练用于人脸关键点检测）的模型添加预处理和后处理，
    输出原始图像尺寸下的边界框、置信度、类别 ID 和关键点数据（不直接绘制）。
    
    后处理步骤说明：
      1. Squeeze 去除 batch 维度
      2. Transpose 将输出转换为 [num_boxes, channels] 格式
      3. Split 将 channels 拆分为 3 部分：bbox (4), confidence (1), keypoints (15) —— 对应 5 个关键点
      4. 使用 NMS 过滤冗余检测
      5. ScaleNMSBoundingBoxesAndKeyPoints 根据原始图像、Resize 和 LetterBox 计算缩放因子，将检测结果转换到原始尺寸
    """
    model, w_in, h_in = _get_model_and_info(input_model_path)
    onnx_opset = 18
    # 新输入定义为 jpg/png 图像字节
    inputs = [create_named_value("image_bytes", onnx.TensorProto.UINT8, ["num_bytes"])]
    pipeline = PrePostProcessor(inputs, onnx_opset)
    
    # 预处理：将图像字节转换为 BGR 图像，再转换为 CHW 格式并归一化，同时调整为模型输入尺寸 640x640
    pre_processing_steps = [
        ConvertImageToBGR(name="BGRImageHWC"),             # 将 jpg/png 转换为 BGR，HWC 排列
        ChannelsLastToChannelsFirst(name="BGRImageCHW"),     # HWC -> CHW
        Resize((h_in, w_in), policy='not_larger', layout='CHW'),
        LetterBox(target_shape=(h_in, w_in), layout='CHW'),
        ImageBytesToFloat(),                               # 像素值归一化到 0..1
        Unsqueeze([0]),                                    # 添加 batch 维度，输出形状变为 [1, 3, 640, 640]
    ]
    pipeline.add_pre_processing(pre_processing_steps)
    
    # 后处理：对模型输出进行处理，使之输出检测结果数据，而非绘制图像
    post_processing_steps = [
        Squeeze([0]),                                    # 去除 batch 维度
        Transpose([1, 0]),                               # 转置为 [num_boxes, channels]
        # 将 channels 拆分为 3 部分：bbox (4), confidence (1), keypoints (15) 
        # 修改 Split 的逻辑，根据模型输出的实际维度进行拆分
        Split(num_outputs=3, axis=1, splits=[4, 1, 15]),
        # 使用 NMS 过滤检测，阈值可根据实际需求调整
        SelectBestBoundingBoxesByNMS(iou_threshold=0.5, score_threshold=0.45, has_mask_data=True),
        # 将经过 NMS 后的边界框和关键点按原始图像尺度进行缩放
        (ScaleNMSBoundingBoxesAndKeyPoints(num_key_points=5, layout='CHW'),
         [
             # 通过 IoMapEntry 将原始图像、Resize 和 LetterBox 的输出传递给缩放算子
             utils.IoMapEntry("BGRImageCHW", producer_idx=0, consumer_idx=1),
             utils.IoMapEntry("Resize", producer_idx=0, consumer_idx=2),
             utils.IoMapEntry("LetterBox", producer_idx=0, consumer_idx=3),
         ]),
    ]
    pipeline.add_post_processing(post_processing_steps)
    
    _update_model(model, output_model_path, pipeline)

if __name__ == "__main__":
    add_pre_post_processing_for_face(Path("yolov8n-face.onnx"), Path("yolov8n-face-pre-post.onnx"))
