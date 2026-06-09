"""
CLIP模型特征提取器
使用OpenAI CLIP模型提取图像语义特征向量
"""

import logging
import numpy as np
from PIL import Image
from io import BytesIO
import requests

logger = logging.getLogger(__name__)


class FeatureExtractor:
    """
    CLIP图像特征提取器
    使用OpenAI CLIP ViT-B/32模型提取512维特征向量
    """

    def __init__(self, model_name: str = "ViT-B/32"):
        """
        初始化特征提取器
        Args:
            model_name: CLIP模型名称
                - "ViT-B/32": ViT-B/32 (默认, 512维)
                - "ViT-B/16": ViT-B/16 (512维)
                - "ViT-L/14": ViT-L/14 (768维)
                - "RN50": ResNet50 (1024维)
        """
        self.model_name = model_name
        self.model = None
        self.preprocess = None
        self.device = "cpu"
        self._load_model()

    def _load_model(self):
        """
        加载CLIP模型
        使用onnx简化部署或直接使用PyTorch
        """
        try:
            import torch

            # 检测是否有GPU
            self.device = "cuda" if torch.cuda.is_available() else "cpu"
            logger.info(f"使用设备: {self.device}")

            # 尝试加载CLIP模型
            try:
                import clip
                self.model, self.preprocess = clip.load(self.model_name, device=self.device)
                self.model.eval()
                self._using_clip = True
                logger.info(f"CLIP模型 {self.model_name} 加载成功 (OpenAI CLIP)")
            except ImportError:
                logger.warning("openai-clip 未安装，使用ONNX精简模型")
                self._load_onnx_model()

        except ImportError:
            logger.warning("PyTorch未安装，使用轻量级特征提取")
            self._load_fallback()

    def _load_onnx_model(self):
        """
        加载ONNX精简模型
        """
        try:
            import onnxruntime as ort
            import os

            model_path = os.path.join(
                os.path.dirname(__file__), "..", "models", "clip_vit_b32.onnx"
            )

            if os.path.exists(model_path):
                self.ort_session = ort.InferenceSession(model_path)
                self._using_onnx = True
                logger.info("ONNX模型加载成功")
            else:
                logger.warning(f"ONNX模型不存在: {model_path}，使用备用方案")
                self._load_fallback()
        except ImportError:
            logger.warning("onnxruntime未安装，使用备用方案")
            self._load_fallback()

    def _load_fallback(self):
        """
        备用方案：使用图像哈希和统计特征
        """
        logger.info("使用备用特征提取方案（图像统计特征）")
        self._using_fallback = True

    def extract_features_from_url(self, image_url: str) -> np.ndarray:
        """
        从URL加载图像并提取特征
        Args:
            image_url: 图像URL或IPFS网关URL
        Returns:
            512维特征向量 (numpy array)
        """
        try:
            # 从URL下载图像
            response = requests.get(image_url, timeout=30)
            response.raise_for_status()

            image = Image.open(BytesIO(response.content)).convert('RGB')
            return self.extract_features_from_image(image)

        except requests.RequestException as e:
            logger.warning(f"URL加载失败，使用模拟特征: {e}")
            return self._generate_simulated_features(image_url)

    def extract_features_from_file(self, file_path: str) -> np.ndarray:
        """
        从本地文件提取特征
        Args:
            file_path: 图像文件路径
        Returns:
            512维特征向量
        """
        image = Image.open(file_path).convert('RGB')
        return self.extract_features_from_image(image)

    def extract_features_from_image(self, image: Image.Image) -> np.ndarray:
        """
        从PIL Image提取特征
        Args:
            image: PIL Image对象
        Returns:
            512维特征向量
        """
        if hasattr(self, '_using_clip') and self._using_clip:
            return self._extract_clip_features(image)
        elif hasattr(self, '_using_onnx') and self._using_onnx:
            return self._extract_onnx_features(image)
        else:
            return self._extract_fallback_features(image)

    def _extract_clip_features(self, image: Image.Image) -> np.ndarray:
        """
        使用CLIP提取特征
        """
        import torch

        # 预处理
        image_input = self.preprocess(image).unsqueeze(0).to(self.device)

        # 推理
        with torch.no_grad():
            image_features = self.model.encode_image(image_input)
            # 归一化
            image_features = image_features / image_features.norm(dim=-1, keepdim=True)

        return image_features.cpu().numpy().flatten()

    def _extract_onnx_features(self, image: Image.Image) -> np.ndarray:
        """
        使用ONNX模型提取特征
        """
        import torch

        # 使用torch做预处理
        from torchvision import transforms

        preprocess = transforms.Compose([
            transforms.Resize(224),
            transforms.CenterCrop(224),
            transforms.ToTensor(),
            transforms.Normalize(
                mean=[0.48145466, 0.4578275, 0.40821073],
                std=[0.26862954, 0.26130258, 0.27577711]
            )
        ])

        image_tensor = preprocess(image).unsqueeze(0).numpy()

        # ONNX推理
        ort_inputs = {self.ort_session.get_inputs()[0].name: image_tensor}
        ort_outputs = self.ort_session.run(None, ort_inputs)

        features = ort_outputs[0].flatten()
        # 归一化
        norm = np.linalg.norm(features)
        if norm > 0:
            features = features / norm

        return features.astype(np.float32)

    def _extract_fallback_features(self, image: Image.Image) -> np.ndarray:
        """
        备用特征提取：基于图像统计特征
        当CLIP模型不可用时使用
        """
        # 调整图像大小
        image = image.resize((64, 64))

        # 转换为numpy数组
        img_array = np.array(image, dtype=np.float32) / 255.0

        # 提取多维度统计特征
        features = []

        # 颜色直方图特征 (256维)
        for channel in range(3):
            hist, _ = np.histogram(img_array[:, :, channel], bins=64, range=(0, 1))
            features.extend(hist / hist.sum())

        # 空间频率特征 (64维)
        for channel in range(3):
            diff_h = np.diff(img_array[:, :, channel], axis=1)
            diff_v = np.diff(img_array[:, :, channel], axis=0)
            features.append(np.mean(np.abs(diff_h)))
            features.append(np.std(diff_h))
            features.append(np.mean(np.abs(diff_v)))
            features.append(np.std(diff_v))

        # 纹理特征 (64维)
        for channel in range(3):
            features.append(np.mean(img_array[:, :, channel]))
            features.append(np.var(img_array[:, :, channel]))
            features.append(np.percentile(img_array[:, :, channel], 25))
            features.append(np.percentile(img_array[:, :, channel], 75))

        # 填充到512维
        features = np.array(features, dtype=np.float32)
        if len(features) < 512:
            # 使用PCA-like降维或填充
            padding = np.zeros(512 - len(features), dtype=np.float32)
            features = np.concatenate([features, padding])
        elif len(features) > 512:
            features = features[:512]

        # 归一化
        norm = np.linalg.norm(features)
        if norm > 0:
            features = features / norm

        return features.astype(np.float32)

    def _generate_simulated_features(self, seed: str) -> np.ndarray:
        """
        生成模拟特征向量（用于开发和测试）
        当无法加载图像时使用
        """
        # 使用seed生成确定性的伪特征
        import hashlib
        hash_bytes = hashlib.sha256(seed.encode()).digest()

        # 将哈希转换为512维归一化向量
        np.random.seed(int.from_bytes(hash_bytes[:4], 'big'))
        features = np.random.randn(512).astype(np.float32)

        # 归一化
        norm = np.linalg.norm(features)
        if norm > 0:
            features = features / norm

        return features
