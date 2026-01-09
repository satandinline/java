<template>
  <div class="search-view-container">
    <div class="search-header">
      <h1 class="search-title">全文/AI检索</h1>
      <div class="mode-switch">
        <button 
          :class="['mode-btn', searchMode === 'full' ? 'active' : '']" 
          @click="switchMode('full')"
          :disabled="isSearching"
        >
          全文检索
        </button>
        <button 
          :class="['mode-btn', searchMode === 'ai' ? 'active' : '']" 
          @click="switchMode('ai')"
          :disabled="isSearching"
        >
          AI检索
        </button>
      </div>
      <div class="search-bar">
        <input 
          type="text" 
          v-model="searchQuery" 
          @keyup.enter="handleSearch"
          placeholder="请输入检索词 (例如：寒食节)..." 
          class="search-input"
        />
        <button class="search-btn" @click="handleSearch" :disabled="isSearching">
          {{ isSearching ? '检索中...' : (searchMode === 'ai' ? 'AI检索' : '全文检索') }}
        </button>
        <button class="back-btn" @click="goBack">返回</button>
      </div>
    </div>

    <!-- 搜索结果区域 -->
    <div class="search-results" v-if="hasSearched">
      <!-- 搜索结果为空的提示 -->
      <div v-if="!isSearching && resourceList.length === 0" class="no-results">
        <p>{{ emptyMessage }}</p>
        <p class="hint">请尝试更换关键词或稍后重试</p>
      </div>

      <!-- 搜索结果列表 -->
      <div v-else class="results-grid">
        <div class="result-item" v-for="item in resourceList" :key="item.id" @click="goToResourceDetail(item)">
          <div class="result-img-container">
            <img 
              v-if="item.image_url" 
              :src="item.image_url" 
              class="result-img" 
              @error="handleImageError($event)"
            />
            <div v-else class="result-img-placeholder">
              <span>暂无图片</span>
            </div>
          </div>
          <div class="result-info">
            <h3 class="result-title">{{ item.entity_name || item.title }}</h3>
            <p class="result-description">{{ item.description || item.snippet }}</p>
            <div class="result-meta">
              <span class="result-tag" v-for="tag in item.tags" :key="tag">{{ tag }}</span>
              <a v-if="item.source_url && item.source_url !== '#'" :href="item.source_url" target="_blank" class="source-link" @click.stop>
                查看来源 →
              </a>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 分页控件 -->
      <div class="pagination" v-if="resourceList.length > 0 && totalPages > 1">
        <button class="page-btn" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">上一页</button>
        <div class="page-numbers">
          <template v-for="pageNum in visiblePages" :key="pageNum">
            <button v-if="pageNum !== '...'" class="page-number" :class="{ active: pageNum === currentPage }" @click="goToPage(pageNum)">{{ pageNum }}</button>
            <span v-else class="page-ellipsis">...</span>
          </template>
        </div>
        <button class="page-btn" :disabled="currentPage === totalPages" @click="goToPage(currentPage + 1)">下一页</button>
        <div class="page-jump">
          <span>跳转到</span>
          <input type="number" v-model.number="jumpPage" :min="1" :max="totalPages" @keyup.enter="jumpToPage" class="page-input" />
          <span>页</span>
          <button class="jump-btn" @click="jumpToPage">确定</button>
        </div>
      </div>

      <!-- AI分析结果展示（如果有） -->
      <div v-if="aiAnalysis && Object.keys(aiAnalysis).length > 0" class="ai-analysis">
        <h3>AI分析结果</h3>
        <div class="analysis-content">
          <p v-if="aiAnalysis.keywords && aiAnalysis.keywords.length > 0">
            <strong>关键词：</strong>{{ aiAnalysis.keywords.join('、') }}
          </p>
          <p v-if="aiAnalysis.advanced_query">
            <strong>优化检索式：</strong>{{ aiAnalysis.advanced_query }}
          </p>
        </div>
      </div>
    </div>

    <!-- 初始状态提示 -->
    <div v-else class="initial-state">
      <div class="welcome-message">
        <h2>欢迎使用AI全文检索</h2>
        <p>请输入关键词进行检索，系统将使用AI技术为您提供精准的搜索结果</p>
        <div class="example-queries">
          <p>示例查询：</p>
          <div class="example-tags">
            <span class="example-tag" @click="searchExample('寒食节')">寒食节</span>
            <span class="example-tag" @click="searchExample('春节')">春节</span>
            <span class="example-tag" @click="searchExample('端午节')">端午节</span>
            <span class="example-tag" @click="searchExample('中秋节')">中秋节</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';

const router = useRouter();
const route = useRoute();

const searchQuery = ref('');
const searchMode = ref('full'); // full | ai
const resourceList = ref([]);
const isSearching = ref(false);
const hasSearched = ref(false);
const aiAnalysis = ref({});
const emptyMessage = ref('抱歉，暂无数据，请稍后重试');
const currentPage = ref(1);
const totalPages = ref(1);
const jumpPage = ref(1);

// 从路由参数获取搜索关键词
onMounted(() => {
  if (route.query.mode === 'ai') {
    searchMode.value = 'ai';
  }
  if (route.query.q) {
    searchQuery.value = route.query.q;
    handleSearch();
  }
});

const switchMode = (mode) => {
  if (mode === searchMode.value) return;
  searchMode.value = mode;
  router.replace({ query: { ...route.query, mode } });
  if (searchQuery.value.trim()) {
    handleSearch(); // 切换模式时自动重查
  }
};

const handleSearch = async (page = 1) => {
  const q = searchQuery.value.trim();
  if (!q) return;

  isSearching.value = true;
  hasSearched.value = true;
  resourceList.value = [];
  aiAnalysis.value = {};
  currentPage.value = page;

  try {
    // 根据模式选择不同的后端接口
    const endpoint = getEndpoint(searchMode.value, q, page, 8);
    const response = await fetch(endpoint);
    
    if (!response.ok) {
      throw new Error(`HTTP错误! 状态: ${response.status}`);
    }
    
    const resData = await response.json();

    if (resData.code === 200) {
      resourceList.value = (resData.data || []).map(item => ({
        id: item.id,
        entity_name: item.title || item.entity_name,
        title: item.title || item.entity_name,
        description: item.snippet || item.description,
        snippet: item.snippet || item.description,
        image_url: item.image_url,
        source_url: item.source_url,
        tags: item.tags || [],
        festival_name: item.festival_name || item.entity_name || item.title,
        table: item.table,
        entity_name: item.entity_name || item.title
      }));
      
      // 更新分页信息
      currentPage.value = resData.page || 1;
      totalPages.value = resData.total_pages || 1;
      jumpPage.value = currentPage.value;
      
      // AI 模式下，优先展示 ai_analysis；全文检索也兼容
      if (resData.ai_analysis) {
        aiAnalysis.value = resData.ai_analysis;
      }

      if (resourceList.value.length === 0) {
        emptyMessage.value = '抱歉，暂无数据，请稍后重试';
      }
    } else {
      emptyMessage.value = '抱歉，暂无数据，请稍后重试';
      alert('搜索出错: ' + (resData.msg || '未知错误'));
    }
  } catch (error) {
    emptyMessage.value = '抱歉，暂无数据，请稍后重试';
    alert('搜索服务连接失败，请检查后端服务是否正常运行');
  } finally {
    isSearching.value = false;
  }
};

// 选择不同检索接口
const getEndpoint = (mode, q, page = 1, pageSize = 8) => {
  const encoded = encodeURIComponent(q);
  if (mode === 'ai') {
    // 优先尝试独立 AI 接口，如不存在可后端兼容 /api/search?mode=ai
    return `/api/ai_search?q=${encoded}&page=${page}&page_size=${pageSize}`;
  }
  // 全文检索：直接数据库相关性
  return `/api/search?q=${encoded}&page=${page}&page_size=${pageSize}`;
};

const searchExample = (query) => {
  searchQuery.value = query;
  handleSearch();
};

const goBack = () => {
  // 使用router.back()返回上一个路由，如果没有历史记录则返回首页
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push('/');
  }
};

const handleImageError = (event) => {
  event.target.style.display = 'none';
  const placeholder = event.target.nextElementSibling;
  if (placeholder) placeholder.style.display = 'flex';
};

// 跳转到资源详情页
const goToResourceDetail = (item) => {
  const festivalName = item.festival_name || item.entity_name || item.title;
  if (festivalName) {
    router.push({
      path: '/resource/detail',
      query: { 
        festival_name: festivalName,
        entity_name: item.entity_name || item.title
      }
    });
  } else if (item.id && item.table) {
    router.push({
      path: '/resource/detail',
      query: {
        id: item.id,
        table: item.table
      }
    });
  }
};

// 分页相关函数
const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value && page !== currentPage.value) {
    handleSearch(page);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
};

const jumpToPage = () => {
  const page = parseInt(jumpPage.value);
  if (page >= 1 && page <= totalPages.value) {
    goToPage(page);
  } else {
    alert(`请输入1到${totalPages.value}之间的页码`);
    jumpPage.value = currentPage.value;
  }
};

const visiblePages = computed(() => {
  const pages = [];
  const current = currentPage.value;
  const total = totalPages.value;
  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i);
  } else {
    if (current <= 4) {
      for (let i = 1; i <= 5; i++) pages.push(i);
      pages.push('...'); pages.push(total);
    } else if (current >= total - 3) {
      pages.push(1); pages.push('...');
      for (let i = total - 4; i <= total; i++) pages.push(i);
    } else {
      pages.push(1); pages.push('...');
      for (let i = current - 2; i <= current + 2; i++) pages.push(i);
      pages.push('...'); pages.push(total);
    }
  }
  return pages;
});
</script>

<style scoped>
.search-view-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 20px;
  min-height: calc(100vh - 100px);
}

.search-header {
  margin-bottom: 40px;
}

.search-title {
  font-size: 32px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 30px;
  text-align: center;
}

.mode-switch {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-bottom: 16px;
}

.mode-btn {
  padding: 10px 20px;
  border: 1px solid #dcdfe6;
  background: #f5f7fa;
  color: #606266;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.mode-btn.active {
  background: #409eff;
  color: #fff;
  border-color: #409eff;
}

.mode-btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.search-bar {
  display: flex;
  gap: 15px;
  justify-content: center;
  align-items: center;
  max-width: 800px;
  margin: 0 auto;
}

.search-input {
  flex: 1;
  padding: 15px 20px;
  border: 2px solid #dcdfe6;
  border-radius: 8px;
  font-size: 16px;
  outline: none;
  transition: border-color 0.3s;
}

.search-input:focus {
  border-color: #409eff;
}

.search-btn {
  padding: 15px 40px;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 500;
  transition: background-color 0.3s;
}

.search-btn:hover:not(:disabled) {
  background-color: #66b1ff;
}

.search-btn:disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
}

.back-btn {
  padding: 15px 25px;
  background-color: #f56c6c;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.3s;
}

.back-btn:hover {
  background-color: #ff4d4f;
}

.search-results {
  margin-top: 40px;
}

.no-results {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}

.no-results p {
  font-size: 18px;
  margin-bottom: 10px;
}

.hint {
  font-size: 14px;
  color: #c0c4cc;
}

.results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  margin-bottom: 40px;
}

.result-item {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  border: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
}

.result-item:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 24px rgba(0,0,0,0.15);
  border-color: #409eff;
}

.result-img-container {
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: #f5f7fa;
  position: relative;
}

.result-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.result-item:hover .result-img {
  transform: scale(1.1);
}

.result-img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 14px;
  background: #f5f7fa;
}

.result-info {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.result-title {
  margin: 0 0 12px;
  font-size: 18px;
  color: #303133;
  font-weight: 600;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}

.result-description {
  margin: 0 0 15px;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  flex: 1;
}

.result-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.result-tag {
  padding: 4px 12px;
  background-color: #ecf5ff;
  color: #409eff;
  border-radius: 4px;
  font-size: 12px;
}

.source-link {
  font-size: 12px;
  color: #409eff;
  text-decoration: none;
  margin-left: auto;
}

.source-link:hover {
  text-decoration: underline;
}

.ai-analysis {
  margin-top: 40px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  border-left: 4px solid #409eff;
}

.ai-analysis h3 {
  margin: 0 0 15px;
  font-size: 18px;
  color: #303133;
}

.analysis-content p {
  margin: 8px 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.initial-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.welcome-message {
  text-align: center;
  max-width: 600px;
}

.welcome-message h2 {
  font-size: 28px;
  color: #303133;
  margin-bottom: 15px;
}

.welcome-message > p {
  font-size: 16px;
  color: #606266;
  margin-bottom: 40px;
  line-height: 1.6;
}

.example-queries {
  margin-top: 30px;
}

.example-queries p {
  font-size: 14px;
  color: #909399;
  margin-bottom: 15px;
}

.example-tags {
  display: flex;
  gap: 10px;
  justify-content: center;
  flex-wrap: wrap;
}

.example-tag {
  padding: 10px 20px;
  background-color: #ecf5ff;
  color: #409eff;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
}

.example-tag:hover {
  background-color: #409eff;
  color: white;
  transform: translateY(-2px);
}

/* 分页控件样式（与首页一致） */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 40px;
  padding: 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.page-btn {
  padding: 8px 16px;
  border: 1px solid #dcdfe6;
  background: white;
  color: #606266;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
}

.page-btn:hover:not(:disabled) {
  color: #409eff;
  border-color: #409eff;
}

.page-btn:disabled {
  color: #c0c4cc;
  cursor: not-allowed;
  background: #f5f7fa;
}

.page-numbers {
  display: flex;
  gap: 8px;
  align-items: center;
}

.page-number {
  min-width: 36px;
  height: 36px;
  padding: 0 8px;
  border: 1px solid #dcdfe6;
  background: white;
  color: #606266;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
}

.page-number:hover {
  color: #409eff;
  border-color: #409eff;
}

.page-number.active {
  background: #409eff;
  color: white;
  border-color: #409eff;
}

.page-ellipsis {
  padding: 0 4px;
  color: #909399;
}

.page-jump {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 20px;
  padding-left: 20px;
  border-left: 1px solid #e4e7ed;
  font-size: 14px;
  color: #606266;
}

.page-input {
  width: 60px;
  height: 36px;
  padding: 0 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  text-align: center;
  font-size: 14px;
}

.page-input:focus {
  outline: none;
  border-color: #409eff;
}

.jump-btn {
  padding: 8px 16px;
  border: 1px solid #409eff;
  background: #409eff;
  color: white;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
}

.jump-btn:hover {
  background: #66b1ff;
  border-color: #66b1ff;
}
</style>
