"""
链创守护 - AI侵权检测服务
基于CLIP模型的语义指纹提取与相似度计算
Flask RESTful API 服务
"""

import json
import logging
import numpy as np
from flask import Flask, request, jsonify
from flask_cors import CORS
from app.feature_extractor import FeatureExtractor
from app.similarity_calculator import SimilarityCalculator

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 创建Flask应用
app = Flask(__name__)
CORS(app)

# 初始化模型
feature_extractor = None
similarity_calculator = SimilarityCalculator()


def get_feature_extractor():
    """懒加载特征提取器"""
    global feature_extractor
    if feature_extractor is None:
        logger.info("正在加载CLIP模型...")
        feature_extractor = FeatureExtractor()
        logger.info("CLIP模型加载完成")
    return feature_extractor


# ============ API 路由 ============

@app.route('/api/health', methods=['GET'])
def health_check():
    """健康检查"""
    return jsonify({
        'status': 'ok',
        'service': 'AIGC Infringement Detection Service',
        'version': '1.0.0',
        'model_loaded': feature_extractor is not None
    })


@app.route('/api/extract-features', methods=['POST'])
def extract_features():
    """
    提取图像特征向量
    请求体: { "ipfsCid": "Qm...", "workId": 123 }
    响应: { "features": [...], "dimension": 512 }
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': '请求体不能为空'}), 400

        ipfs_cid = data.get('ipfsCid')
        work_id = data.get('workId')

        if not ipfs_cid:
            return jsonify({'error': '缺少ipfsCid参数'}), 400

        logger.info(f"提取特征: workId={work_id}, cid={ipfs_cid}")

        # 从IPFS获取图像数据（此处使用模拟数据，实际需要IPFS客户端）
        # 由于Flask服务通常无法直接访问IPFS，可使用HTTP网关
        image_url = f"http://ipfs:8080/ipfs/{ipfs_cid}"

        # 提取CLIP特征
        extractor = get_feature_extractor()
        features = extractor.extract_features_from_url(image_url)

        # 将numpy数组转换为列表
        features_list = features.tolist() if isinstance(features, np.ndarray) else features

        logger.info(f"特征提取成功: workId={work_id}, dimension={len(features_list)}")

        return jsonify({
            'success': True,
            'workId': work_id,
            'features': features_list,
            'dimension': len(features_list)
        })

    except Exception as e:
        logger.error(f"特征提取失败: {str(e)}", exc_info=True)
        return jsonify({'error': f'特征提取失败: {str(e)}'}), 500


@app.route('/api/calculate-similarity', methods=['POST'])
def calculate_similarity():
    """
    计算两个特征向量的余弦相似度
    请求体: { "vector1": [...], "vector2": [...] }
    响应: { "similarity": 0.95, "isSimilar": true }
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': '请求体不能为空'}), 400

        vector1_str = data.get('vector1')
        vector2_str = data.get('vector2')

        if not vector1_str or not vector2_str:
            return jsonify({'error': '缺少特征向量参数'}), 400

        # 解析向量
        v1 = _parse_vector(vector1_str)
        v2 = _parse_vector(vector2_str)

        if v1 is None or v2 is None:
            return jsonify({'error': '特征向量格式错误'}), 400

        # 计算余弦相似度
        similarity = similarity_calculator.cosine_similarity(v1, v2)

        logger.info(f"相似度计算: {similarity:.4f}")

        return jsonify({
            'success': True,
            'similarity': float(similarity),
            'isSimilar': similarity >= 0.85
        })

    except Exception as e:
        logger.error(f"相似度计算失败: {str(e)}", exc_info=True)
        return jsonify({'error': f'相似度计算失败: {str(e)}'}), 500


@app.route('/api/batch-similarity', methods=['POST'])
def batch_similarity():
    """
    批量计算相似度
    请求体: { "targetVector": [...], "compareVectors": [[...], [...], ...] }
    响应: { "results": [{ "index": 0, "similarity": 0.95 }, ...] }
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': '请求体不能为空'}), 400

        target_str = data.get('targetVector')
        compare_list = data.get('compareVectors', [])

        if not target_str or not compare_list:
            return jsonify({'error': '缺少参数'}), 400

        # 解析目标向量
        target = _parse_vector(target_str)
        if target is None:
            return jsonify({'error': '目标特征向量格式错误'}), 400

        # 批量计算相似度
        results = []
        for i, vec_str in enumerate(compare_list):
            vec = _parse_vector(vec_str)
            if vec is not None:
                sim = similarity_calculator.cosine_similarity(target, vec)
                results.append({
                    'index': i,
                    'similarity': float(sim),
                    'isSimilar': sim >= 0.85
                })

        # 按相似度降序排序
        results.sort(key=lambda x: x['similarity'], reverse=True)

        logger.info(f"批量相似度计算完成: {len(results)} 条结果")

        return jsonify({
            'success': True,
            'totalCompared': len(compare_list),
            'similarCount': sum(1 for r in results if r['isSimilar']),
            'results': results
        })

    except Exception as e:
        logger.error(f"批量计算失败: {str(e)}", exc_info=True)
        return jsonify({'error': f'批量计算失败: {str(e)}'}), 500


@app.route('/api/detect-infringement', methods=['POST'])
def detect_infringement():
    """
    侵权检测接口（端到端）
    请求体: { "workId": 123, "ipfsCid": "Qm...", "referenceVectors": { "1": [...], "2": [...] } }
    响应: { "workId": 123, "similarWorks": [{ "refWorkId": 1, "similarity": 0.95 }, ...] }
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': '请求体不能为空'}), 400

        work_id = data.get('workId')
        ipfs_cid = data.get('ipfsCid')
        reference_vectors = data.get('referenceVectors', {})

        if not work_id or not ipfs_cid:
            return jsonify({'error': '缺少必要参数'}), 400

        logger.info(f"执行侵权检测: workId={work_id}")

        # 提取目标作品特征
        extractor = get_feature_extractor()
        image_url = f"http://ipfs:8080/ipfs/{ipfs_cid}"
        target_features = extractor.extract_features_from_url(image_url)
        target_list = target_features.tolist()

        # 与参考向量逐一比对
        similar_works = []
        for ref_id, ref_vec in reference_vectors.items():
            ref_array = np.array(ref_vec)
            similarity = similarity_calculator.cosine_similarity(target_list, ref_array)

            if similarity >= 0.85:
                similar_works.append({
                    'refWorkId': int(ref_id),
                    'similarity': round(float(similarity) * 100, 2)
                })

        # 按相似度排序
        similar_works.sort(key=lambda x: x['similarity'], reverse=True)

        logger.info(f"侵权检测完成: 发现 {len(similar_works)} 件相似作品")

        return jsonify({
            'success': True,
            'workId': work_id,
            'similarWorks': similar_works,
            'totalCompared': len(reference_vectors),
            'similarCount': len(similar_works)
        })

    except Exception as e:
        logger.error(f"侵权检测失败: {str(e)}", exc_info=True)
        return jsonify({'error': f'侵权检测失败: {str(e)}'}), 500


def _parse_vector(vector_input):
    """
    解析特征向量（支持JSON字符串、列表等多种格式）
    """
    if isinstance(vector_input, list):
        return np.array(vector_input, dtype=np.float32)
    elif isinstance(vector_input, str):
        try:
            # 尝试JSON解析
            parsed = json.loads(vector_input)
            if isinstance(parsed, list):
                return np.array(parsed, dtype=np.float32)
        except (json.JSONDecodeError, ValueError):
            # 尝试解析为逗号分隔的数值
            try:
                values = [float(x.strip()) for x in vector_input.strip('[]').split(',')]
                return np.array(values, dtype=np.float32)
            except ValueError:
                pass
    return None


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
