import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页 - 链创守护' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录 - 链创守护' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册 - 链创守护' }
  },
  {
    path: '/upload',
    name: 'Upload',
    component: () => import('@/views/UploadWork.vue'),
    meta: { title: '上传作品 - 链创守护', requiresAuth: true }
  },
  {
    path: '/works/:id',
    name: 'WorkDetail',
    component: () => import('@/views/WorkDetail.vue'),
    meta: { title: '作品详情 - 链创守护' }
  },
  {
    path: '/my-works',
    name: 'MyWorks',
    component: () => import('@/views/MyWorks.vue'),
    meta: { title: '我的作品 - 链创守护', requiresAuth: true }
  },
  {
    path: '/copyright/:id',
    name: 'CopyrightDetail',
    component: () => import('@/views/CopyrightDetail.vue'),
    meta: { title: '版权证书 - 链创守护' }
  },
  {
    path: '/my-nfts',
    name: 'MyNFTs',
    component: () => import('@/views/MyNFTs.vue'),
    meta: { title: '我的NFT - 链创守护', requiresAuth: true }
  },
  {
    path: '/detection',
    name: 'Detection',
    component: () => import('@/views/Detection.vue'),
    meta: { title: '侵权检测 - 链创守护', requiresAuth: true }
  },
  {
    path: '/authorization',
    name: 'Authorization',
    component: () => import('@/views/Authorization.vue'),
    meta: { title: '授权交易 - 链创守护', requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { title: '个人中心 - 链创守护', requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = to.meta.title || '链创守护'

  const token = localStorage.getItem('accessToken')
  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
