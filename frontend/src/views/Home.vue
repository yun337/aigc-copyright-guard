<template>
  <div class="home">
    <!-- 英雄区 -->
    <section class="hero">
      <div class="hero-tag">
        <span class="tag-dot"></span> Ethereum + IPFS + CLIP AI
      </div>
      <h1 class="hero-title">
        为每一个 <span class="gradient-text">AIGC 作品</span><br/>
        铸造不可篡改的数字灵魂
      </h1>
      <p class="hero-desc">
        基于以太坊区块链与AI深度语义模型，为创作者提供
        <strong>版权自动存证</strong>、<strong>侵权智能检测</strong>和<strong>链上授权交易</strong>的一站式保护方案
      </p>
      <div class="hero-btns">
        <button class="btn-main" @click="$router.push('/upload')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>
          </svg>
          上传作品开始保护
        </button>
        <button class="btn-outline" @click="$router.push('/detection')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          侵权检测
        </button>
      </div>

      <!-- 数据卡片 -->
      <div class="stats">
        <div class="stat-item">
          <div class="stat-num">{{ stats.works }}</div>
          <div class="stat-label">链上存证</div>
        </div>
        <div class="stat-item">
          <div class="stat-num">{{ stats.nfts }}</div>
          <div class="stat-label">版权NFT</div>
        </div>
        <div class="stat-item">
          <div class="stat-num">{{ stats.detections }}</div>
          <div class="stat-label">AI检测</div>
        </div>
        <div class="stat-item">
          <div class="stat-num">{{ stats.auths }}</div>
          <div class="stat-label">授权交易</div>
        </div>
      </div>
    </section>

    <!-- 核心功能 -->
    <section class="section">
      <div class="section-head">
        <div>
          <h2 class="section-title">核心能力</h2>
          <p class="section-sub">从创作确权到检测交易，全链路保护</p>
        </div>
      </div>

      <div class="feature-grid">
        <div class="feature-card" v-for="f in features" :key="f.title" @click="$router.push(f.route)">
          <div class="feature-icon" :style="{ background: f.gradient }">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8" stroke-linecap="round" v-html="f.icon"></svg>
          </div>
          <h3>{{ f.title }}</h3>
          <p>{{ f.desc }}</p>
          <span class="feature-link">{{ f.action }} →</span>
        </div>
      </div>
    </section>

    <!-- 最新作品 -->
    <section class="section" v-if="recentWorks.length">
      <div class="section-head">
        <h2 class="section-title">最新存证作品</h2>
        <span class="view-all" @click="$router.push('/my-works')">浏览全部 →</span>
      </div>
      <div class="work-grid">
        <div class="work-card" v-for="w in recentWorks" :key="w.id" @click="$router.push(`/works/${w.id}`)">
          <div class="work-cover" :style="{ background: cardGradient(w) }">
            <!-- IMAGE: IPFS加载，失败显示fallback文字 -->
            <template v-if="w.workType === 'IMAGE'">
              <img v-if="!mediaErrors[w.id] && w.ipfsCid" :src="ipfsUrl(w.ipfsCid)" class="work-img" @error="mediaErrors[w.id] = true" :alt="w.title" />
              <span v-if="mediaErrors[w.id] || !w.ipfsCid" class="work-cover-text">
                <span class="type-emoji">{{ typeEmoji(w.workType) }}</span>
                {{ w.title?.substring(0, 8) || w.workType }}
              </span>
            </template>
            <!-- VIDEO: IPFS加载，失败显示fallback -->
            <template v-else-if="w.workType === 'VIDEO'">
              <video v-if="!mediaErrors[w.id] && w.ipfsCid" :src="ipfsUrl(w.ipfsCid)" class="work-img" @error="mediaErrors[w.id] = true" />
              <span v-if="mediaErrors[w.id] || !w.ipfsCid" class="work-cover-text">
                <span class="type-emoji">{{ typeEmoji(w.workType) }}</span>
                {{ w.title?.substring(0, 8) || w.workType }}
              </span>
            </template>
            <!-- 其他类型: 直接显示emoji+标题 -->
            <span v-else class="work-cover-text">
              <span class="type-emoji">{{ typeEmoji(w.workType) }}</span>
              {{ w.title?.substring(0, 8) || w.workType }}
            </span>
            <div class="work-cover-pattern"></div>
          </div>
          <div class="work-body">
            <h4>{{ w.title }}</h4>
            <p class="work-author">{{ w.user?.username || '匿名创作者' }}</p>
            <span class="work-tag" :class="w.copyrightStatus === 'REGISTERED' ? 'tag-ok' : 'tag-pend'">
              {{ w.copyrightStatus === 'REGISTERED' ? '✓ 已存证' : '待存证' }}
            </span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { workAPI } from '@/api'

const stats = ref({ works: 0, nfts: 0, detections: 0, auths: 0 })
const recentWorks = ref([])
const mediaErrors = reactive({})

const gradients = [
  'linear-gradient(135deg, #6C5CE7, #A29BFE)',
  'linear-gradient(135deg, #00CEC9, #81ECEC)',
  'linear-gradient(135deg, #FD79A8, #FDCB6E)',
  'linear-gradient(135deg, #00B894, #55EFC4)',
  'linear-gradient(135deg, #0984E3, #74B9FF)',
]

const features = [
  {
    title: '版权存证', action: '开始存证',
    desc: 'SHA256文件哈希 + 时间戳写入以太坊链，生成不可篡改的版权证明，链上永久可查',
    gradient: 'linear-gradient(135deg, #6C5CE7, #A29BFE)',
    route: '/upload',
    icon: '<path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>'
  },
  {
    title: 'NFT铸造', action: '铸造NFT',
    desc: 'ERC-721标准NFT绑定作品版权，支持版税设置，在OpenSea等平台自由交易',
    gradient: 'linear-gradient(135deg, #00CEC9, #81ECEC)',
    route: '/my-nfts',
    icon: '<polygon points="12 2 22 8.5 22 15.5 12 22 2 15.5 2 8.5 12 2"/><line x1="12" y1="22" x2="12" y2="15.5"/><polyline points="22 8.5 12 15.5 2 8.5"/>'
  },
  {
    title: 'AI侵权检测', action: '开始检测',
    desc: 'CLIP语义指纹提取 × 余弦相似度比对，精准识别抄袭与高度相似作品',
    gradient: 'linear-gradient(135deg, #E17055, #FDCB6E)',
    route: '/detection',
    icon: '<circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="8" y1="11" x2="14" y2="11"/>'
  },
  {
    title: '授权交易', action: '管理授权',
    desc: '链上授权合约记录许可关系，独家/非独家/临时授权，关系不可否认',
    gradient: 'linear-gradient(135deg, #00B894, #55EFC4)',
    route: '/authorization',
    icon: '<path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z"/><path d="M9 12l2 2 4-4"/>'
  }
]

onMounted(async () => {
  try {
    const res = await workAPI.getList(0, 8)
    if (res.data?.code === 200) {
      recentWorks.value = res.data.data.content || []
      stats.value.works = res.data.data.totalElements || 0
    }
  } catch (_) {}
})

function ipfsUrl(cid) { return cid ? `http://localhost:8081/ipfs/${cid}` : '' }
function typeEmoji(t) { return { IMAGE: '🖼', VIDEO: '🎬', MUSIC: '🎵', TEXT: '📄' }[t] || '' }

function cardGradient(w) {
  const colors = [
    ['#6C5CE7', '#A29BFE'],
    ['#00CEC9', '#55EFC4'],
    ['#E17055', '#FDCB6E'],
    ['#0984E3', '#74B9FF'],
    ['#00B894', '#00CEC9'],
    ['#A29BFE', '#FD79A8'],
    ['#636E72', '#B2BEC3'],
    ['#6C5CE7', '#00CEC9'],
  ]
  const idx = (w.id || 0) % colors.length
  return `linear-gradient(135deg, ${colors[idx][0]}, ${colors[idx][1]})`
}
</script>

<style scoped>
/* ===== 英雄区 ===== */
.hero {
  text-align: center;
  padding: 60px 0 48px;
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 18px;
  border-radius: 100px;
  background: var(--gradient-light);
  border: 1px solid rgba(108,92,231,0.15);
  font-size: 13px;
  color: var(--brand-purple);
  font-weight: 500;
  margin-bottom: 28px;
}

.tag-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: var(--brand-cyan);
}

.hero-title {
  font-size: 44px;
  font-weight: 900;
  line-height: 1.25;
  letter-spacing: -1px;
  color: var(--text-primary);
  margin-bottom: 20px;
}

.gradient-text {
  background: var(--gradient-brand);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-desc {
  max-width: 600px;
  margin: 0 auto 36px;
  font-size: 15px;
  line-height: 1.8;
  color: var(--text-secondary);
}

.hero-desc strong { color: var(--text-primary); font-weight: 600; }

.hero-btns {
  display: flex;
  justify-content: center;
  gap: 14px;
  margin-bottom: 56px;
}

.btn-main {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 30px;
  border: none;
  border-radius: 100px;
  background: var(--gradient-brand);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 16px rgba(108,92,231,0.3);
}

.btn-main:hover { transform: translateY(-2px); box-shadow: 0 6px 24px rgba(108,92,231,0.4); }

.btn-outline {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 30px;
  border: 2px solid var(--border);
  border-radius: 100px;
  background: var(--bg-white);
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-outline:hover { border-color: var(--brand-purple); color: var(--brand-purple); }

/* ===== 统计 ===== */
.stats {
  display: flex;
  justify-content: center;
  gap: 48px;
}

.stat-num {
  font-size: 36px;
  font-weight: 800;
  background: var(--gradient-brand);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.stat-label { font-size: 13px; color: var(--text-muted); margin-top: 2px; }

/* ===== 通用区域 ===== */
.section { margin-top: 64px; }

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 28px;
}

.section-title { font-size: 24px; font-weight: 800; }

.section-sub { font-size: 13px; color: var(--text-muted); margin-top: 4px; }

.view-all { font-size: 14px; color: var(--brand-purple); cursor: pointer; font-weight: 500; }
.view-all:hover { opacity: .8; }

/* ===== 功能卡片 ===== */
.feature-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }

.feature-card {
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 28px 24px;
  cursor: pointer;
  transition: all 0.3s;
}

.feature-card:hover {
  border-color: var(--brand-purple);
  box-shadow: var(--shadow-lg);
  transform: translateY(-3px);
}

.feature-icon {
  width: 48px; height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.feature-card h3 { font-size: 17px; font-weight: 700; margin-bottom: 8px; }

.feature-card p { font-size: 13px; color: var(--text-secondary); line-height: 1.6; margin-bottom: 14px; }

.feature-link { font-size: 13px; font-weight: 600; color: var(--brand-purple); }

/* ===== 作品卡片 ===== */
.work-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }

.work-card {
  background: var(--bg-white);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.work-card:hover {
  border-color: var(--brand-purple);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.work-cover {
  height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.work-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  z-index: 1;
}

.work-cover-pattern {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 20% 50%, rgba(255,255,255,0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(255,255,255,0.08) 0%, transparent 40%),
    radial-gradient(circle at 40% 20%, rgba(255,255,255,0.06) 0%, transparent 30%);
}

.work-cover-text {
  position: relative;
  z-index: 1;
  font-size: 18px;
  font-weight: 800;
  color: rgba(255,255,255,0.85);
  text-shadow: 0 2px 4px rgba(0,0,0,0.2);
  letter-spacing: 2px;
  pointer-events: none;
}

.work-body { padding: 16px; }

.work-body h4 {
  font-size: 15px; font-weight: 600;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  margin-bottom: 4px;
}

.work-author { font-size: 12px; color: var(--text-muted); margin-bottom: 10px; }

.work-tag { font-size: 11px; padding: 3px 10px; border-radius: 100px; font-weight: 600; }

.tag-ok  { background: rgba(0,184,148,0.1); color: #00B894; }
.tag-pend { background: rgba(245,158,11,0.1); color: #E17055; }

@media (max-width: 1024px) {
  .feature-grid, .work-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .hero { padding: 40px 0; }
  .hero-title { font-size: 28px; }
  .stats { gap: 24px; flex-wrap: wrap; }
  .stat-num { font-size: 28px; }
  .feature-grid, .work-grid { grid-template-columns: 1fr; }
}
</style>
