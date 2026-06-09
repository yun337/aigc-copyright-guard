<template>
  <div class="nfts-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <h2><el-icon><Collection /></el-icon> 我的 NFT</h2>
          <span class="nft-count" v-if="nfts.length">共 {{ nfts.length }} 个</span>
        </div>
      </template>

      <div class="work-grid" v-if="nfts.length">
        <div class="work-card" v-for="n in nfts" :key="n.id" @click="$router.push(`/works/${n.work?.id}`)">
          <div class="work-cover" :style="{ background: cardGradient(n.id) }">
            <img v-if="n.work?.ipfsCid" :src="ipfsUrl(n.work.ipfsCid)" class="work-img" @error="onErr" />
            <div class="work-pattern"></div>
            <span v-if="!n.work?.ipfsCid" class="work-text">{{ n.work?.title?.substring(0, 8) || 'NFT' }}</span>
          </div>
          <div class="work-body">
            <h4>{{ n.work?.title || '未命名' }}</h4>
            <p class="token-id">Token #{{ n.tokenId }}</p>
            <div class="work-meta">
              <span class="meta-tag tag-nft">NFT</span>
              <span class="meta-contract">{{ truncate(n.contractAddress) }}</span>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-else description="还没有铸造 NFT">
        <el-button type="primary" @click="$router.push('/upload')">去上传作品</el-button>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { nftAPI } from '@/api'

const nfts = ref([])

const gradients = [
  ['#6C5CE7','#A29BFE'],['#00CEC9','#55EFC4'],['#E17055','#FDCB6E'],
  ['#0984E3','#74B9FF'],['#00B894','#00CEC9'],['#A29BFE','#FD79A8'],
  ['#6C5CE7','#00CEC9'],['#636E72','#B2BEC3'],
]

onMounted(async () => {
  try {
    const res = await nftAPI.getMyNFTs()
    if (res.data?.code === 200) nfts.value = res.data.data || []
  } catch (e) { console.error(e) }
})

function ipfsUrl(cid) { return `http://localhost:8081/ipfs/${cid}` }
function onErr(e) { e.target.style.display = 'none' }
function cardGradient(id) {
  const c = gradients[(id||0) % gradients.length]
  return `linear-gradient(135deg, ${c[0]}, ${c[1]})`
}
function truncate(s) { return s ? s.slice(0,8)+'...'+s.slice(-6) : '' }
</script>

<style scoped>
.nfts-page { max-width: 1200px; margin: 0 auto; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.nft-count { font-size: 14px; color: #909399; }

.work-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }

.work-card {
  background: #fff; border: 1px solid #E8E9F0; border-radius: 14px;
  overflow: hidden; cursor: pointer; transition: all .3s;
}
.work-card:hover { border-color: #6C5CE7; box-shadow: 0 4px 16px rgba(108,92,231,.1); transform: translateY(-2px); }

.work-cover {
  height: 160px; display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
}
.work-img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: contain; z-index: 1; }
.work-pattern {
  position: absolute; inset: 0;
  background: radial-gradient(circle at 30% 40%, rgba(255,255,255,.1) 0%, transparent 50%),
              radial-gradient(circle at 70% 60%, rgba(255,255,255,.06) 0%, transparent 40%);
}
.work-text { position: relative; z-index: 1; font-size: 18px; font-weight: 800; color: rgba(255,255,255,.85); letter-spacing: 2px; }

.work-body { padding: 16px; }
.work-body h4 { font-size: 15px; font-weight: 600; margin-bottom: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.token-id { font-size: 12px; color: #E6A23C; font-weight: 600; margin-bottom: 10px; }
.work-meta { display: flex; justify-content: space-between; align-items: center; }
.meta-tag { font-size: 11px; padding: 2px 10px; border-radius: 100px; font-weight: 600; }
.tag-nft { background: rgba(230,162,60,.1); color: #E6A23C; }
.meta-contract { font-size: 11px; color: #909399; font-family: monospace; }

@media (max-width: 1024px) { .work-grid { grid-template-columns: repeat(2,1fr); } }
@media (max-width: 768px)  { .work-grid { grid-template-columns: 1fr; } }
</style>
