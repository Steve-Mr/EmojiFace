import onnx
from onnxruntime.quantization import quantize_dynamic, QuantType
import sys
import os

def quantize_model(input_model_path, output_model_path):
    print(f"Quantizing model: {input_model_path} -> {output_model_path}")

    # 动态量化: 将权重转换为 INT8
    # 这种方法不需要 calibration data，适合大多数情况，能显著减小模型体积
    quantize_dynamic(
        input_model_path,
        output_model_path,
        weight_type=QuantType.QUInt8
    )

    print("Quantization complete.")

    # 打印大小对比
    orig_size = os.path.getsize(input_model_path) / (1024 * 1024)
    quant_size = os.path.getsize(output_model_path) / (1024 * 1024)
    print(f"Original Size: {orig_size:.2f} MB")
    print(f"Quantized Size: {quant_size:.2f} MB")
    print(f"Reduction: {(1 - quant_size/orig_size)*100:.1f}%")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python3 quantize_model.py <input_model.onnx> <output_model.onnx>")
        sys.exit(1)

    input_path = sys.argv[1]
    output_path = sys.argv[2]

    if not os.path.exists(input_path):
        print(f"Error: Input file {input_path} not found.")
        sys.exit(1)

    quantize_model(input_path, output_path)
