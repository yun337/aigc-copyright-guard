"""
相似度计算器
计算图像特征向量之间的相似度
"""

import numpy as np
from typing import List, Tuple, Optional
import logging

logger = logging.getLogger(__name__)


class SimilarityCalculator:
    """
    向量相似度计算器
    支持多种相似度计算方法
    """

    def __init__(self):
        pass

    def cosine_similarity(self, vector1: np.ndarray, vector2: np.ndarray) -> float:
        """
        计算余弦相似度
        Args:
            vector1: 特征向量1
            vector2: 特征向量2
        Returns:
            余弦相似度 [0, 1]，1表示完全相同
        """
        # 确保是numpy数组
        v1 = np.array(vector1, dtype=np.float32).flatten()
        v2 = np.array(vector2, dtype=np.float32).flatten()

        # 检查维度一致
        if v1.shape != v2.shape:
            logger.warning(f"向量维度不一致: {v1.shape} vs {v2.shape}")
            # 取最小维度
            min_dim = min(len(v1), len(v2))
            v1 = v1[:min_dim]
            v2 = v2[:min_dim]

        # 计算余弦相似度
        dot_product = np.dot(v1, v2)
        norm1 = np.linalg.norm(v1)
        norm2 = np.linalg.norm(v2)

        if norm1 == 0 or norm2 == 0:
            return 0.0

        similarity = dot_product / (norm1 * norm2)

        # 确保结果在[0, 1]范围内
        return float(max(0.0, min(1.0, similarity)))

    def euclidean_similarity(self, vector1: np.ndarray, vector2: np.ndarray) -> float:
        """
        基于欧氏距离的相似度
        转换为[0, 1]范围
        """
        v1 = np.array(vector1, dtype=np.float32).flatten()
        v2 = np.array(vector2, dtype=np.float32).flatten()

        distance = np.linalg.norm(v1 - v2)
        # 转换为相似度: 1/(1+distance)
        return float(1.0 / (1.0 + distance))

    def manhattan_similarity(self, vector1: np.ndarray, vector2: np.ndarray) -> float:
        """
        基于曼哈顿距离的相似度
        """
        v1 = np.array(vector1, dtype=np.float32).flatten()
        v2 = np.array(vector2, dtype=np.float32).flatten()

        distance = np.sum(np.abs(v1 - v2))
        return float(1.0 / (1.0 + distance))

    def pearson_correlation(self, vector1: np.ndarray, vector2: np.ndarray) -> float:
        """
        皮尔逊相关系数
        """
        v1 = np.array(vector1, dtype=np.float32).flatten()
        v2 = np.array(vector2, dtype=np.float32).flatten()

        if len(v1) < 2 or len(v2) < 2:
            return self.cosine_similarity(v1, v2)

        # 去均值
        v1_centered = v1 - np.mean(v1)
        v2_centered = v2 - np.mean(v2)

        # 计算相关系数
        numerator = np.sum(v1_centered * v2_centered)
        denominator = np.sqrt(np.sum(v1_centered ** 2) * np.sum(v2_centered ** 2))

        if denominator == 0:
            return 0.0

        correlation = numerator / denominator
        # 映射到[0, 1]
        return float((correlation + 1.0) / 2.0)

    def find_top_k_similar(
        self,
        target: np.ndarray,
        reference_vectors: List[Tuple[str, np.ndarray]],
        k: int = 10,
        threshold: float = 0.0
    ) -> List[dict]:
        """
        从参考向量集中找到Top-K最相似的
        Args:
            target: 目标特征向量
            reference_vectors: 参考向量列表 [(id, vector), ...]
            k: 返回前K个
            threshold: 相似度阈值
        Returns:
            [{ "id": ..., "similarity": ... }, ...]
        """
        results = []

        for ref_id, ref_vec in reference_vectors:
            sim = self.cosine_similarity(target, ref_vec)
            if sim >= threshold:
                results.append({
                    'id': ref_id,
                    'similarity': round(sim * 100, 2)
                })

        # 按相似度降序排序
        results.sort(key=lambda x: x['similarity'], reverse=True)

        return results[:k]

    def batch_cosine_similarity(
        self,
        target: np.ndarray,
        reference_matrix: np.ndarray
    ) -> np.ndarray:
        """
        批量计算余弦相似度（向量化，性能更优）
        Args:
            target: 目标向量 (dim,)
            reference_matrix: 参考矩阵 (N, dim)
        Returns:
            相似度数组 (N,)
        """
        target = np.array(target, dtype=np.float32).flatten()
        reference_matrix = np.array(reference_matrix, dtype=np.float32)

        # 归一化
        target_norm = target / (np.linalg.norm(target) + 1e-8)
        ref_norms = reference_matrix / (np.linalg.norm(reference_matrix, axis=1, keepdims=True) + 1e-8)

        # 批量点积
        similarities = np.dot(ref_norms, target_norm)

        return np.clip(similarities, 0, 1)
