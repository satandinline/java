<template>
  <div class="mm-page">
    <div class="mm-header">
      <div class="mm-header-content">
        <div class="mm-title">图文互搜</div>
        <div class="mm-subtitle">输入文本或上传图片，检索相关文本/图片资源</div>
      </div>
    </div>

    <div class="mm-search-container">
      <div class="mm-inputs">
        <textarea
          v-model="mmQuery"
          class="text-input"
          rows="4"
          placeholder="请输入要检索的文本，或上传图片进行以图搜文/图"
          @keydown.enter.prevent="handleEnterKey"
          @keydown.ctrl.enter="performMultimodalSearch"
        ></textarea>
        
        <div class="mm-actions">
          <div class="mm-upload-section">
            <button
              class="upload-btn"
              @click="() => $refs.mmImageInput && $refs.mmImageInput.click()"
            >
              📷 上传图片
            </button>
            <input
              ref="mmImageInput"
              type="file"
              accept="image/*"
              multiple
              @change="handleMMImageUpload"
              style="display: none;"
            />
          </div>
          
          <button
            class="send-btn"
            @click="performMultimodalSearch"
            :disabled="mmLoading || (!mmQuery.trim() && mmUploadedImages.length === 0)"
          >
            <span v-if="mmLoading">🔍 检索中...</span>
            <span v-else>✨ 开始互搜</span>
          </button>
        </div>
      </div>

      <div v-if="mmUploadedImages.length > 0" class="uploaded-images mm">
        <div class="mm-images-grid">
          <div
            v-for="(item, index) in mmUploadedImages"
            :key="index"
            class="uploaded-image-item"
          >
            <img :src="item.preview" class="preview-img" />
            <button class="remove-btn" @click="removeMMImage(index)">×</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="mmError" class="mm-error">{{ mmError }}</div>

    <div v-if="mmResults" class="mm-results">
      <div class="mm-meta">
        <div>🔍 使用的查询：{{ mmResults.query_used }}</div>
        <div v-if="mmResults.image_descriptions?.length">
          📝 图片描述：{{ mmResults.image_descriptions.join('；') }}
        </div>
      </div>

      <div class="mm-results-container">
        <!-- 向量结果 -->
        <div class="mm-result-column">
          <div class="mm-card mm-card-vector">
            <div class="mm-card-title">
              <div class="card-title-content">
                <span class="card-icon">🔍</span>
                <span class="card-text">向量结果</span>
              </div>
            </div>
            <div v-if="mmResults.vector_results?.length" class="mm-results-list">
              <div
                v-for="(item, idx) in mmResults.vector_results"
                :key="idx"
                class="mm-item mm-item-clickable"
                @click="goToResourceDetail(item)"
              >
                <div class="mm-title">{{ item.title || '向量检索结果' }}</div>
                <div class="mm-text">{{ item.content }}</div>
                <div class="mm-meta-line">来源: {{ item.source || item.table || '向量库' }}</div>
              </div>
            </div>
            <div v-else class="mm-empty">暂无向量结果</div>
          </div>
        </div>

        <!-- 文字结果 -->
        <div class="mm-result-column">
          <div class="mm-card mm-card-text">
            <div class="mm-card-title">
              <div class="card-title-content">
                <span class="card-icon">📝</span>
                <span class="card-text">文字结果</span>
              </div>
            </div>
            <div v-if="mmResults.text_results?.length" class="mm-results-list">
              <div
                v-for="(item, idx) in mmResults.text_results"
                :key="idx"
                class="mm-item mm-item-clickable"
                @click="goToResourceDetail(item)"
              >
                <div class="mm-title">{{ item.title || item.table || '记录' }}</div>
                <div class="mm-text">{{ item.content }}</div>
                <div class="mm-meta-line">来源: {{ item.source || item.table || '数据库' }}</div>
              </div>
            </div>
            <div v-else class="mm-empty">暂无文字结果</div>
          </div>
        </div>

        <!-- 图片结果 -->
        <div class="mm-result-column">
          <div class="mm-card mm-card-image">
            <div class="mm-card-title">
              <div class="card-title-content">
                <span class="card-icon">🖼️</span>
                <span class="card-text">图片结果</span>
              </div>
            </div>
            <div v-if="mmResults.image_results?.length" class="mm-results-list">
              <div
                v-for="(item, idx) in mmResults.image_results"
                :key="idx"
                class="mm-item mm-item-clickable"
                @click="goToResourceDetail(item)"
              >
                <div v-if="item.image_url" class="mm-image">
                  <img
                    :src="getImageUrl(item.image_url)"
                    class="mm-img"
                    @click.stop="previewImage(getImageUrl(item.image_url))"
                  />
                </div>
                <div class="mm-title">{{ item.title || '未命名' }}</div>
                <div class="mm-text">{{ item.content || '' }}</div>
                <div class="mm-meta-line">来源: {{ item.source || item.table || '数据库' }}</div>
              </div>
            </div>
            <div v-else class="mm-empty">暂无图片结果</div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="previewImageUrl" class="image-preview-modal" @click="previewImageUrl = null">
      <img :src="previewImageUrl" class="preview-modal-image" @click.stop />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { getCurrentUser } from '../utils/api.js';

const router = useRouter();

const mmQuery = ref('');
const mmUploadedImages = ref([]);
const mmResults = ref(null);
const mmLoading = ref(false);
const mmError = ref('');
const previewImageUrl = ref(null);

const handleEnterKey = (event) => {
  // 如果按下了Shift+Enter，则换行
  if (event.shiftKey) {
    // 不做任何操作，允许默认换行行为
    return;
  }
  // 如果按下了Enter但没有按Ctrl，则不提交，只换行
  // 如果按下了Ctrl+Enter，则触发搜索（已在模板中绑定）
};

const handleMMImageUpload = (event) => {
  const files = event.target.files;
  if (files && files.length > 0) {
    Array.from(files).forEach(file => {
      if (file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = (e) => {
          mmUploadedImages.value.push({ file, preview: e.target.result });
        };
        reader.readAsDataURL(file);
      }
    });
  }
};

const removeMMImage = (index) => {
  mmUploadedImages.value.splice(index, 1);
};

const performMultimodalSearch = async () => {
  if (mmLoading.value) return;
  const queryText = mmQuery.value.trim();
  if (!queryText && mmUploadedImages.value.length === 0) {
    alert('请输入查询文本或上传图片');
    return;
  }

  const currentUser = getCurrentUser();
  if (!currentUser || !currentUser.id) {
    alert('请先登录');
    return;
  }

  mmLoading.value = true;
  mmError.value = '';
  mmResults.value = null;

  const formData = new FormData();
  formData.append('mode', 'text');
  formData.append('query', queryText);
  formData.append('user_id', currentUser.id);
  mmUploadedImages.value.forEach((item) => {
    formData.append('images', item.file);
  });

  try {
    const resp = await fetch('/api/multimodal/search', {
      method: 'POST',
      headers: {
        'X-User-Id': currentUser.id.toString()
      },
      body: formData
    });
    const text = await resp.text();
    let data = null;
    try {
      data = text ? JSON.parse(text) : null;
    } catch (parseErr) {
      mmError.value = '后端返回非JSON，请检查服务器日志';
      return;
    }
    if (!resp.ok || !data || data.success === false) {
      mmError.value = data?.message || '检索失败';
      return;
    }
    mmResults.value = data;
  } catch (err) {
    mmError.value = err?.message || '请求失败';
  } finally {
    mmLoading.value = false;
  }
};

const previewImage = (url) => {
  // 确保URL是完整的API路径
  let fullUrl = url;
  if (url && !url.startsWith('http') && !url.startsWith('/api/images/') && !url.startsWith('/default.jpg')) {
    // 如果URL不包含已知的API路径，尝试构建正确的路径
    if (url.includes('AIGC_graph')) {
      const fileName = url.split('/').pop();
      fullUrl = `/api/images/aigc/${fileName}`;
    } else if (url.includes('crawled_images')) {
      const fileName = url.split('/').pop();
      fullUrl = `/api/images/crawled/${fileName}`;
    } else if (url.includes('uploads') || url.includes('image_from_users')) {
      const fileName = url.split('/').pop();
      fullUrl = `/api/images/user/${fileName}`;
    } else {
      // 如果只是文件名，尝试多个可能的路径
      const fileName = url.split('/').pop();
      fullUrl = `/api/images/crawled/${fileName}`; // 默认路径
    }
  }
  previewImageUrl.value = fullUrl;
};

// 获取图片URL
const getImageUrl = (url) => {
  if (!url) return '/default.jpg';
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }
  if (url.startsWith('/')) {
    return url;
  }
  if (url.includes('crawled_images')) {
    const fileName = url.split('/').pop();
    return `/api/images/crawled/${fileName}`;
  }
  if (url.includes('AIGC_graph')) {
    const fileName = url.split('/').pop();
    return `/api/images/aigc/${fileName}`;
  }
  if (url.includes('uploads')) {
    const fileName = url.split('/').pop();
    return `/api/images/user/${fileName}`;
  }
  // 如果路径中包含文件扩展名，尝试直接作为文件名处理
  if (url.match(/\.(jpg|jpeg|png|gif|bmp|webp)$/i)) {
    const fileName = url.split('/').pop();
    // 尝试多个可能的路径
    const possiblePaths = [
      `/api/images/crawled/${fileName}`,
      `/api/images/aigc/${fileName}`,
      `/api/images/user/${fileName}`,
      `/default.jpg`
    ];
    
    // 这里我们返回第一个可能的路径，后端需要能够处理这些路径
    for (const path of possiblePaths) {
      if (path.includes(fileName)) {
        return path;
      }
    }
  }
  return url;
};

// 跳转到资源详情页
const goToResourceDetail = (item) => {
  // 对于图片向量搜索结果（table为image_vector_store），无法跳转到详情页
  if (item.table === 'image_vector_store') {
    return;
  }
  
  // 对于crawled_images和AIGC_graph表，优先使用festival_name或entity_name
  if (item.table === 'crawled_images' || item.table === 'AIGC_graph') {
    // 优先使用festival_name或entity_name
    const festivalName = item.festival_name || item.entity_name || item.title || '';
    if (festivalName) {
      router.push({
        path: '/resource/detail',
        query: {
          festival_name: festivalName
        }
      });
      return;
    }
    // 如果没有festival_name，尝试使用id+table方式
    if (item.id) {
      let realId = item.id;
      if (typeof item.id === 'string') {
        const idMatch = item.id.match(/_(\d+)$/);
        if (idMatch) {
          realId = parseInt(idMatch[1]);
        } else {
          realId = parseInt(item.id);
        }
      }
      
      if (!isNaN(realId)) {
        router.push({
          path: '/resource/detail',
          query: {
            id: realId,
            table: item.table
          }
        });
        return;
      }
    }
    // 如果都没有，不跳转
    return;
  }
  
  // 优先使用festival_name或entity_name（最可靠的方式）
  if (item.festival_name || item.entity_name || item.title) {
    router.push({
      path: '/resource/detail',
      query: {
        festival_name: item.festival_name || item.entity_name || item.title || ''
      }
    });
    return;
  }
  
  // 如果有table参数且不是image_vector_store，使用id+table方式
  if (item.table && item.table !== 'image_vector_store' && item.id) {
    // 从id中提取真实ID（如果格式是img_xxx或entity_xxx）
    let realId = item.id;
    if (typeof item.id === 'string') {
      const idMatch = item.id.match(/_(\d+)$/);
      if (idMatch) {
        realId = parseInt(idMatch[1]);
      } else {
        realId = parseInt(item.id);
      }
    }
    
    if (!isNaN(realId)) {
      const query = {
        id: realId,
        table: item.table,
        resource_type: item.resource_type || ''
      };
      router.push({
        path: '/resource/detail',
        query: query
      });
      return;
    }
  }
  
  // 如果都没有，尝试使用title
  if (item.title) {
    router.push({
      path: '/resource/detail',
      query: {
        festival_name: item.title
      }
    });
  }
};
</script>

<style scoped>
.mm-page {
  max-width: 1400px;
  margin: 20px auto;
  padding: 24px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.1);
}

.mm-header {
  text-align: center;
  margin-bottom: 24px;
}

.mm-header-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.mm-title {
  font-size: 28px;
  font-weight: 700;
  color: #222;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.mm-subtitle {
  font-size: 15px;
  color: #666;
}

.mm-search-container {
  margin-bottom: 24px;
}

.mm-inputs {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.text-input {
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  padding: 16px;
  resize: vertical;
  font-size: 15px;
  min-height: 140px;
  transition: border-color 0.3s;
  outline: none;
}

.text-input:focus {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.mm-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.mm-upload-section {
  flex: 1;
  min-width: 150px;
}

.upload-btn {
  width: 100%;
  padding: 12px 16px;
  border: 2px dashed #dcdfe6;
  border-radius: 10px;
  cursor: pointer;
  background: #fafafa;
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.upload-btn:hover {
  border-color: #409eff;
  background: #ecf5ff;
  color: #409eff;
}

.send-btn {
  min-width: 140px;
  height: 48px;
  background: linear-gradient(135deg, #409eff, #66b1ff);
  border: none;
  color: #fff;
  border-radius: 12px;
  cursor: pointer;
  font-weight: 600;
  padding: 0 24px;
  font-size: 15px;
  line-height: 1;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.4);
}

.send-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.uploaded-images.mm {
  display: flex;
  margin: 16px 0;
}

.mm-images-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
  width: 100%;
}

.uploaded-image-item {
  position: relative;
  width: 100%;
  height: 120px;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  transition: transform 0.3s;
}

.uploaded-image-item:hover {
  transform: scale(1.05);
}

.preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  top: 6px;
  right: 6px;
  background: rgba(0,0,0,0.7);
  color: #fff;
  border: none;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  transition: background 0.3s;
}

.remove-btn:hover {
  background: rgba(0,0,0,0.9);
}

.mm-error {
  color: #e74c3c;
  margin: 12px 0;
  padding: 12px;
  background: #fef0f0;
  border-radius: 8px;
  border-left: 4px solid #e74c3c;
}

.mm-results {
  margin-top: 24px;
}

.mm-meta {
  font-size: 14px;
  color: #555;
  margin-bottom: 16px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
}

.mm-results-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 24px;
}

@media (max-width: 1200px) {
  .mm-results-container {
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  }
}

@media (max-width: 768px) {
  .mm-results-container {
    grid-template-columns: 1fr;
  }
  
  .mm-page {
    padding: 16px;
    margin: 10px;
  }
}

.mm-result-column {
  width: 100%;
}

.mm-card {
  background: #ffffff;
  border: 1px solid #e6ecf5;
  border-radius: 12px;
  padding: 16px;
  height: 100%;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.03);
  transition: all 0.3s;
}

.mm-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.mm-card-vector {
  border-top: 4px solid #409eff;
}

.mm-card-text {
  border-top: 4px solid #67c23a;
}

.mm-card-image {
  border-top: 4px solid #e6a23c;
}

.mm-card-title {
  font-weight: 700;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.card-title-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-icon {
  font-size: 18px;
}

.card-text {
  font-size: 16px;
}

.mm-item {
  padding: 12px;
  border-radius: 8px;
  background: #fafcff;
  margin-bottom: 12px;
  border: 1px solid #eef2f7;
  transition: all 0.3s;
}

.mm-item:last-child {
  margin-bottom: 0;
}

.mm-item-clickable {
  cursor: pointer;
  transition: all 0.2s;
}

.mm-item-clickable:hover {
  background: #e8f3ff;
  border-color: #409eff;
  transform: translateY(-1px);
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.15);
}

.mm-results-list {
  max-height: 500px;
  overflow-y: auto;
}

/* 滚动条样式 */
.mm-results-list::-webkit-scrollbar {
  width: 6px;
}

.mm-results-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.mm-results-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.mm-results-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.mm-title {
  font-weight: 600;
  margin-bottom: 6px;
  color: #222;
  font-size: 15px;
}

.mm-text {
  color: #555;
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 6px;
}

.mm-meta-line {
  font-size: 12px;
  color: #888;
  margin-top: 4px;
}

.mm-image {
  margin: 8px 0;
}

.mm-img {
  max-width: 100%;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.3s;
}

.mm-img:hover {
  transform: scale(1.02);
}

.mm-empty {
  color: #999;
  font-size: 14px;
  text-align: center;
  padding: 24px 0;
  font-style: italic;
}

.image-preview-modal {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(5px);
}

.preview-modal-image {
  max-width: 90vw;
  max-height: 90vh;
  border-radius: 8px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.5);
  animation: zoomIn 0.3s ease-out;
}

@keyframes zoomIn {
  from {
    transform: scale(0.9);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
