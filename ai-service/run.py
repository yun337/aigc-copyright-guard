"""
链创守护 - AI侵权检测服务启动脚本
"""

import os
import sys

# 添加项目路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from app.main import app

if __name__ == '__main__':
    port = int(os.environ.get('AI_SERVICE_PORT', 5000))
    debug = os.environ.get('AI_SERVICE_DEBUG', 'False').lower() == 'true'

    print("=" * 50)
    print("  链创守护 - AI侵权检测服务")
    print("  Version: 1.0.0")
    print(f"  Port: {port}")
    print(f"  Debug: {debug}")
    print("=" * 50)

    app.run(
        host='0.0.0.0',
        port=port,
        debug=debug,
        threaded=True
    )
