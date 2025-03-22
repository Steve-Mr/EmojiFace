import math
import onnxruntime as ort
import numpy as np
from onnxruntime_extensions import get_library_path
import cv2

def compute_face_roll_angle(keypoints):
    """
    计算人脸的 roll 角度（左右眼连线的角度）
    假设 keypoints 的顺序为 [左眼, 右眼, 鼻尖, 左嘴角, 右嘴角]，
    每个关键点的格式为 [x, y, confidence]。
    返回的角度单位为度（degree）。
    """
    left_eye = keypoints[0][:2]
    right_eye = keypoints[1][:2]
    dx = right_eye[0] - left_eye[0]
    dy = right_eye[1] - left_eye[1]
    angle = math.degrees(math.atan2(dy, dx))
    return angle

def draw_angle_visualization(image, left_eye, right_eye, roll_angle, arrow_length=50):
    """
    在图像上绘制用于可视化人脸朝向的箭头和角度信息。
    
    参数：
      image：原始图像
      left_eye, right_eye：左右眼的坐标 (x, y)
      roll_angle：人脸旋转角度（度）
      arrow_length：箭头的长度（像素）
    """
    # 计算左右眼的中点
    mid_eye = ((left_eye[0] + right_eye[0]) / 2, (left_eye[1] + right_eye[1]) / 2)
    mid_eye = (int(mid_eye[0]), int(mid_eye[1]))

    # 根据角度计算箭头终点坐标
    angle_rad = math.radians(roll_angle)
    end_point = (int(mid_eye[0] + arrow_length * math.cos(angle_rad)),
                 int(mid_eye[1] + arrow_length * math.sin(angle_rad)))

    # 绘制左右眼连线作为参考（颜色为浅蓝色）
    cv2.line(image, (int(left_eye[0]), int(left_eye[1])), (int(right_eye[0]), int(right_eye[1])), (255, 255, 0), 2)
    # 绘制箭头（颜色为红色）
    cv2.arrowedLine(image, mid_eye, end_point, (0, 0, 255), 2, tipLength=0.2)
    # 标注角度
    cv2.putText(image, f"Roll: {roll_angle:.2f} deg", (mid_eye[0] - 50, mid_eye[1] - 10),
                cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)
    return image

# 加载集成了预处理和后处理的模型
model_path = "yolov8n-face-pre-post.onnx"
so = ort.SessionOptions()
ortext_lib_path = get_library_path()
so.register_custom_ops_library(ortext_lib_path)
session = ort.InferenceSession(model_path, so)

# 以二进制方式读取图像文件
image_path = "/home/maary/Build/yolov8-face-landmarks-opencv-dnn/images/4.png"
with open(image_path, "rb") as f:
    image_bytes = np.frombuffer(f.read(), dtype=np.uint8)

# 模型输入的名称为 "image_bytes"，直接传入原始图像字节
inputs = {"image_bytes": image_bytes}

# 运行推理，模型内部会自动执行预处理和后处理步骤
outputs = session.run(None, inputs)

# 输出结果的 shape 为 (num_boxes, 21)，包含边界框、置信度、类别（如果有）和关键点数据
print("预测结果：")
for detection in outputs[0]:
    print(detection)

detections = outputs[0]  # shape: (num_boxes, 21)
image = cv2.imread(image_path)
if image is None:
    raise FileNotFoundError(f"无法加载图片：{image_path}")

# 遍历每个检测
for detection in detections:
    # 提取边界框、置信度、类别和关键点信息
    # 假设 detection 前 4 个数值是 [x, y, width, height]
    bbox = detection[0:4]
    # 假设 detection 前 4 个数值为 [x_center, y_center, width, height]
    x_center, y_center, w, h = detection[0:4]
    x1 = int(x_center - w / 2)
    y1 = int(y_center - h / 2)
    x2 = int(x_center + w / 2)
    y2 = int(y_center + h / 2)

    cv2.rectangle(image, (x1, y1), (x2, y2), color=(0, 255, 0), thickness=2)

    conf = detection[4]
    class_id = int(detection[5])
    # 剩余 15 个数值为 5 个关键点，每个关键点包含 [x, y, confidence]
    keypoints = detection[6:].reshape(5, 3)

    # 显示置信度信息
    label = f"{conf:.2f}"
    cv2.putText(image, label, (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 
                fontScale=0.5, color=(0, 255, 0), thickness=1)
    
    roll_angle = compute_face_roll_angle(keypoints)
    # 调用函数将角度以箭头的形式可视化
    left_eye = keypoints[0][:2]
    right_eye = keypoints[1][:2]
    image = draw_angle_visualization(image, left_eye, right_eye, roll_angle)

    # 绘制关键点
    for (kp_x, kp_y, kp_conf) in keypoints:
        kp_x, kp_y = int(kp_x), int(kp_y)
        cv2.circle(image, (kp_x, kp_y), radius=2, color=(0, 0, 255), thickness=-1)

cv2.imwrite("visualized_output.jpg", image)
