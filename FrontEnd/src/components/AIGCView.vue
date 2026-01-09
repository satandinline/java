<template>
  <div class="aigc-container">
    <!-- 顶部论坛入口 -->
    <div class="forum-header">
      <button class="forum-btn" @click="toggleComments">
        <span class="forum-icon">💬</span>
        评论
      </button>
    </div>

    <!-- 评论面板 -->
    <div v-if="showComments" class="comments-panel">
      <CommentSection 
        :resource-id="currentResourceId"
        :user-id="currentUserInfo?.id"
        @close="showComments = false"
      />
    </div>

    <div class="aigc-layout">
      <!-- 左侧：历史会话导航 -->
      <div class="session-nav-panel" :class="{ collapsed: isHistoryCollapsed }">
        <div class="session-nav-header">
          <h3>历史会话</h3>
          <div class="header-actions">
            <button 
              class="toggle-history-btn" 
              @click="toggleHistoryPanel"
              :title="isHistoryCollapsed ? '显示历史记录' : '隐藏历史记录'"
            >
              <span v-if="isHistoryCollapsed">▶</span>
              <span v-else>◀</span>
            </button>
            <button 
              v-if="!isHistoryCollapsed"
              class="new-chat-btn" 
              @click="createNewSession" 
              title="开启新对话"
            >
              <span>+</span>
            </button>
          </div>
        </div>
        <!-- 删除操作栏 -->
        <div class="delete-actions" v-if="!isHistoryCollapsed && sessionHistory.length > 0">
          <button 
            class="select-all-btn" 
            @click="toggleSelectAll"
            :title="isAllSelected ? '取消全选' : '全选'"
          >
            {{ isAllSelected ? '取消全选' : '全选' }}
          </button>
          <button 
            v-if="selectedSessions.length > 0"
            class="clear-selection-btn" 
            @click="clearSelection"
            title="取消选中"
          >
            取消选中
          </button>
          <button 
            class="delete-btn" 
            @click="deleteSelectedSessions"
            :disabled="selectedSessions.length === 0"
            title="删除选中的会话"
          >
            删除选中 ({{ selectedSessions.length }})
          </button>
          <button 
            class="delete-all-btn" 
            @click="deleteAllSessions"
            title="删除所有会话"
          >
            全部删除
          </button>
        </div>
        <div class="session-list" ref="sessionListRef" v-show="!isHistoryCollapsed">
          <!-- 文字AIGC历史记录 -->
          <div class="history-section">
            <div class="history-section-header" @click="textHistoryExpanded = !textHistoryExpanded">
              <span>📝 文字AIGC历史记录</span>
              <span class="expand-icon">{{ textHistoryExpanded ? '▼' : '▶' }}</span>
            </div>
            <div v-show="textHistoryExpanded" class="history-section-content">
              <div 
                v-for="(session, index) in textSessions" 
                :key="session.id"
                :class="['session-item', { active: currentSessionId === session.id, selected: selectedSessions.includes(session.id) }]"
                @click="handleSessionClick(session.id, $event)"
              >
                <input 
                  type="checkbox" 
                  class="session-checkbox"
                  :checked="selectedSessions.includes(session.id)"
                  @click.stop="toggleSessionSelection(session.id)"
                />
                <div class="session-content" @click="loadSession(session.id)">
                  <div class="session-title">{{ session.title || `会话 ${index + 1}` }}</div>
                  <div class="session-time">{{ formatTime(session.created_at) }}</div>
                  <div class="session-preview">{{ getSessionPreview(session) }}</div>
                </div>
              </div>
              <div v-if="textSessions.length === 0" class="empty-sessions">
                暂无文字AIGC历史记录
              </div>
            </div>
          </div>
          
          <!-- 图片AIGC历史记录 -->
          <div class="history-section">
            <div class="history-section-header" @click="imageHistoryExpanded = !imageHistoryExpanded">
              <span>🎨 图片AIGC历史记录</span>
              <span class="expand-icon">{{ imageHistoryExpanded ? '▼' : '▶' }}</span>
            </div>
            <div v-show="imageHistoryExpanded" class="history-section-content">
              <div 
                v-for="(session, index) in imageSessions" 
                :key="session.id"
                :class="['session-item', { active: currentSessionId === session.id, selected: selectedSessions.includes(session.id) }]"
                @click="handleSessionClick(session.id, $event)"
              >
                <input 
                  type="checkbox" 
                  class="session-checkbox"
                  :checked="selectedSessions.includes(session.id)"
                  @click.stop="toggleSessionSelection(session.id)"
                />
                <div class="session-content" @click="loadSession(session.id)">
                  <div class="session-title">{{ session.title || `会话 ${index + 1}` }}</div>
                  <div class="session-time">{{ formatTime(session.created_at) }}</div>
                  <div class="session-preview">{{ getSessionPreview(session) }}</div>
                </div>
              </div>
              <div v-if="imageSessions.length === 0" class="empty-sessions">
                暂无图片AIGC历史记录
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：当前对话和输入区域 -->
      <div class="main-panel">
        <!-- 当前对话显示区域 -->
        <div class="conversation-area" ref="conversationAreaRef">
          <div 
            v-for="(msg, index) in currentConversation" 
            :key="index"
            :class="['message-item', msg.role]"
          >
            <div class="message-avatar">
              <img 
                v-if="msg.role === 'user' && currentUserAvatar" 
                :src="getAvatarUrl(currentUserAvatar)" 
                class="avatar-img"
                alt="用户头像"
                @error="handleAvatarError"
              />
              <span v-else-if="msg.role === 'user'">👤</span>
              <span v-else>🤖</span>
            </div>
            <div class="message-content-wrapper">
              <div class="message-role-label">{{ msg.role === 'user' ? (currentUserNickname || '用户') : (msg.model === 'image' ? 'Huoshan' : 'Tongyi') }}</div>
              <!-- AI回答时显示左右分栏 -->
              <div v-if="msg.role === 'assistant' && (msg.retrieved_resources || msg.retrieved_resource_ids)" class="ai-response-layout">
                <!-- 左侧：检索到的资源（持久化显示） -->
                <div class="resources-panel">
                  <div class="panel-header">
                    <span>📚 检索资源</span>
                    <button 
                      v-if="msg.retrieved_resource_ids && msg.retrieved_resource_ids.length > 0"
                      class="toggle-resources-btn"
                      @click="toggleResourceExpanded(msg)"
                      :title="isResourceExpanded(msg) ? '折叠' : '展开'"
                    >
                      {{ isResourceExpanded(msg) ? '▼' : '▶' }}
                    </button>
                  </div>
                  <div class="resources-content" v-show="isResourceExpanded(msg) || !msg.retrieved_resource_ids">
                    <!-- 向量库结果 -->
                    <div v-if="msg.retrieved_resources && msg.retrieved_resources.vector_results && msg.retrieved_resources.vector_results.length > 0" class="resource-section">
                      <div class="resource-section-title">向量库检索</div>
                      <div 
                        v-for="(item, idx) in msg.retrieved_resources.vector_results" 
                        :key="`vec-${idx}`"
                        class="resource-item"
                      >
                        <div class="resource-content">{{ item.content }}</div>
                      </div>
                    </div>
                    <!-- 数据库结果 -->
                    <div v-if="msg.retrieved_resources && msg.retrieved_resources.database_results && msg.retrieved_resources.database_results.length > 0" class="resource-section">
                      <div class="resource-section-title">数据库检索</div>
                      <div 
                        v-for="(item, idx) in msg.retrieved_resources.database_results" 
                        :key="`db-${idx}`"
                        class="resource-item"
                        @click="goToResourceDetailFromRetrieved(item)"
                        style="cursor: pointer;"
                      >
                        <div class="resource-source">{{ item.table || '数据库' }}</div>
                        <div v-if="item.title" class="resource-title clickable-resource-title">{{ item.title }}</div>
                        <div v-if="item.content" class="resource-content">{{ item.content }}</div>
                        <div v-if="item.source" class="resource-source-url">{{ item.source }}</div>
                        <div v-if="item.storage_path || item.image_path" class="resource-image">
                          <img :src="getResourceImageUrl(item.storage_path || item.image_path, item.table)" class="resource-img" @click.stop="previewImage(getResourceImageUrl(item.storage_path || item.image_path, item.table))" />
                        </div>
                      </div>
                    </div>
                    <!-- 网页爬取结果 -->
                    <div v-if="msg.retrieved_resources && msg.retrieved_resources.web_results && msg.retrieved_resources.web_results.length > 0" class="resource-section">
                      <div class="resource-section-title">网页检索</div>
                      <div 
                        v-for="(item, idx) in msg.retrieved_resources.web_results" 
                        :key="`web-${idx}`"
                        class="resource-item"
                      >
                        <div v-if="item.title" class="resource-title">{{ item.title }}</div>
                        <div class="resource-content">{{ item.content }}</div>
                        <div v-if="item.source" class="resource-source-url">
                          <a :href="item.source" target="_blank">{{ item.source }}</a>
                        </div>
                      </div>
                    </div>
                    <div v-if="msg.retrieved_resource_ids && msg.retrieved_resource_ids.length > 0 && (!msg.retrieved_resources || (!msg.retrieved_resources.vector_results?.length && !msg.retrieved_resources.database_results?.length && !msg.retrieved_resources.web_results?.length))" class="no-resources">
                      检索资源已保存，点击展开查看详情
                    </div>
                    <div v-else-if="!msg.retrieved_resources || (!msg.retrieved_resources.vector_results?.length && !msg.retrieved_resources.database_results?.length && !msg.retrieved_resources.web_results?.length)" class="no-resources">
                      未检索到相关资源
                    </div>
                  </div>
                  <!-- 折叠状态：只显示资源实体名称列表（支持滑动查看） -->
                  <div v-if="!isResourceExpanded(msg) && msg.retrieved_resource_ids && msg.retrieved_resource_ids.length > 0" class="resources-collapsed">
                    <div class="collapsed-resources-scroll">
                      <div 
                        v-for="(item, idx) in (msg.retrieved_resources?.database_results || [])" 
                        :key="`collapsed-${idx}`"
                        class="collapsed-resource-item"
                        @click="goToResourceDetailFromRetrieved(item)"
                        :title="item.title || '未命名资源'"
                      >
                        <span class="collapsed-resource-name">{{ item.title || '未命名资源' }}</span>
                        <span class="collapsed-resource-type">{{ item.table || '资源' }}</span>
                      </div>
                      <!-- 如果没有加载资源详情，只显示资源ID数量 -->
                      <div v-if="!msg.retrieved_resources?.database_results && msg.retrieved_resource_ids.length > 0" class="collapsed-resource-item">
                        <span class="collapsed-resource-name">已检索到 {{ msg.retrieved_resource_ids.length }} 个资源</span>
                        <span class="collapsed-resource-type">点击展开查看</span>
                      </div>
                    </div>
                  </div>
                </div>
                <!-- 右侧：AI生成的答案 -->
                <div class="answer-panel">
                  <div class="panel-header">💡 AI回答</div>
                  <div class="answer-content">
                    <div v-if="msg.content" class="message-text" v-html="formatAnswerText(msg.content)"></div>
                    <div v-else class="message-text" style="color: #999; font-style: italic;">AI正在思考中...</div>
                    <!-- 连环画：显示多张图片 -->
                    <div v-if="msg.is_comic && msg.image_paths && msg.image_paths.length > 0" class="message-comic-result">
                      <div class="comic-header">连环画（共{{ msg.comic_count || msg.image_paths.length }}张）</div>
                      <div class="comic-images">
                        <img 
                          v-for="(imgPath, imgIdx) in msg.image_paths" 
                          :key="imgIdx"
                          :src="getImageUrl(imgPath)" 
                          class="comic-image" 
                          @click="previewComicImage(imgPath, msg.image_paths, imgIdx)"
                          :alt="`连环画第${imgIdx + 1}张`"
                        />
                      </div>
                    </div>
                    <!-- 单张图片 -->
                    <div v-else-if="msg.image_path" class="message-image-result">
                      <img :src="getImageUrl(msg.image_path)" class="result-image" @click="previewImage(getImageUrl(msg.image_path))" />
                    </div>
                    <div v-if="msg.key_entities && msg.key_entities.length > 0" class="key-entities">
                      <div class="entities-label">关键实体：</div>
                      <div class="entities-list">
                        <span v-for="(entity, idx) in msg.key_entities" :key="idx" class="entity-tag">{{ entity }}</span>
                      </div>
                    </div>
                    <div v-if="msg.sources" class="sources-info">
                      <div class="sources-label">参考来源：</div>
                      <div class="sources-text">{{ msg.sources }}</div>
                    </div>
                  </div>
                </div>
              </div>
              <!-- 用户消息或没有资源的AI消息 -->
              <div v-else class="message-content">
                <div v-if="msg.images && msg.images.length > 0" class="message-images">
                  <img 
                    v-for="(img, imgIdx) in msg.images" 
                    :key="imgIdx"
                    :src="img"
                    class="message-image"
                    @click="previewImage(img)"
                  />
                </div>
                <div v-if="msg.role === 'user' && msg.content" class="message-text" v-html="formatAnswerText(msg.content)"></div>
                <div v-else-if="msg.role === 'assistant' && msg.content" class="message-text" v-html="formatAnswerText(msg.content)"></div>
                <!-- 连环画：显示多张图片 -->
                <div v-if="msg.is_comic && msg.image_paths && msg.image_paths.length > 0" class="message-comic-result">
                  <div class="comic-header">连环画（共{{ msg.comic_count || msg.image_paths.length }}张）</div>
                  <div class="comic-images">
                    <img 
                      v-for="(imgPath, imgIdx) in msg.image_paths" 
                      :key="imgIdx"
                      :src="getImageUrl(imgPath)" 
                      class="comic-image" 
                      @click="previewImage(getImageUrl(imgPath), msg.image_paths.map(p => getImageUrl(p)), imgIdx)"
                      :alt="`连环画第${imgIdx + 1}张`"
                    />
                  </div>
                </div>
                <!-- 单张图片 -->
                <div v-else-if="msg.image_path" class="message-image-result">
                  <img :src="getImageUrl(msg.image_path)" class="result-image" @click="previewImage(getImageUrl(msg.image_path))" />
                </div>
              </div>
              <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
            </div>
          </div>
          <div v-if="currentConversation.length === 0" class="empty-conversation">
            <div class="empty-icon">💬</div>
            <div class="empty-text">开始新的对话吧</div>
          </div>
        </div>

        <!-- 输入区域（页面中部） -->
        <div class="input-area">
          <!-- 模式切换和图片上传 -->
          <div class="input-toolbar">
            <div class="mode-switch">
              <button 
                :class="['mode-btn', { active: aigcMode === 'text' }]"
                @click="aigcMode = 'text'"
              >
                📝 文字AIGC
              </button>
              <button 
                :class="['mode-btn', { active: aigcMode === 'image' }]"
                @click="aigcMode = 'image'"
              >
                🎨 图片AIGC
              </button>
              <button 
                class="mode-btn secondary-creation-btn"
                @click="goToSecondaryCreation"
                title="二次创作专区"
              >
                ✨ 二次创作
              </button>
            </div>
            <label class="upload-btn">
              <input 
                type="file" 
                accept="image/*" 
                multiple 
                @change="handleImageUpload"
                style="display: none;"
              />
              <span class="upload-icon">📷</span>
              上传图片
            </label>
          </div>

          <!-- 已上传的图片预览 -->
          <div v-if="uploadedImages.length > 0" class="uploaded-images">
            <div 
              v-for="(img, index) in uploadedImages" 
              :key="index"
              class="uploaded-image-item"
            >
              <img :src="img" class="preview-img" />
              <button class="remove-btn" @click="removeImage(index)">×</button>
            </div>
          </div>

          <!-- 文本输入和发送 -->
          <div class="text-input-section">
            <textarea
              v-model="userInput"
              :placeholder="aigcMode === 'text' ? '请输入您的问题或需求...' : '请输入图片生成提示词...'"
              class="text-input"
              @keydown.enter.exact.prevent="sendMessage"
              @keydown.enter.shift.exact="userInput += '\n'"
            ></textarea>
            <button 
              v-if="!isLoading"
              class="send-btn" 
              @click="sendMessage"
              :disabled="!userInput.trim() && uploadedImages.length === 0"
            >
              发送
            </button>
            <button 
              v-else
              class="send-btn stop-btn" 
              @click="cancelGeneration"
            >
              停止生成
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 图片预览模态框（支持前后切换） -->
    <div v-if="previewImageUrl" class="image-preview-modal" @click="previewImageUrl = null">
      <div class="preview-modal-content" @click.stop>
        <!-- 关闭按钮 -->
        <button class="preview-close-btn" @click="previewImageUrl = null">×</button>
        <!-- 上一张按钮 -->
        <button 
          v-if="previewImageList.length > 1 && previewImageIndex > 0"
          class="preview-nav-btn preview-prev-btn" 
          @click.stop="prevImage"
          title="上一张 (←)"
        >
          ‹
        </button>
        <!-- 图片 -->
        <img :src="previewImageUrl" class="preview-modal-image" />
        <!-- 下一张按钮 -->
        <button 
          v-if="previewImageList.length > 1 && previewImageIndex < previewImageList.length - 1"
          class="preview-nav-btn preview-next-btn" 
          @click.stop="nextImage"
          title="下一张 (→)"
        >
          ›
        </button>
        <!-- 图片索引指示器 -->
        <div v-if="previewImageList.length > 1" class="preview-indicator">
          {{ previewImageIndex + 1 }} / {{ previewImageList.length }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import CommentSection from './CommentSection.vue';

const router = useRouter();

// 跳转到二次创作专区
const goToSecondaryCreation = () => {
  router.push('/secondary-creation');
};

// 导入统一的getCurrentUser函数
import { getCurrentUser } from '../utils/api.js';

// 获取用户昵称和头像
const currentUserInfo = computed(() => getCurrentUser());
const currentUserNickname = computed(() => currentUserInfo.value?.nickname || currentUserInfo.value?.account || '用户');
const currentUserAvatar = computed(() => currentUserInfo.value?.avatar_path || './default.jpg');

// 获取头像URL
const getAvatarUrl = (avatarPath) => {
  if (!avatarPath) return '/default.jpg';
  if (avatarPath.startsWith('http://') || avatarPath.startsWith('https://')) {
    return avatarPath;
  }
  if (avatarPath.startsWith('/')) {
    return avatarPath;
  }
  if (avatarPath.startsWith('./')) {
    return avatarPath.replace('./', '/');
  }
  return '/' + avatarPath;
};

// 处理头像加载错误
const handleAvatarError = (event) => {
  event.target.src = '/default.jpg';
};

// 会话管理
const sessionHistory = ref([]);
const currentSessionId = ref(null);
const currentConversation = ref([]);
const selectedSessions = ref([]); // 选中的会话ID列表
const isHistoryCollapsed = ref(false); // 历史记录面板是否折叠
const textHistoryExpanded = ref(true); // 文字AIGC历史记录是否展开
const imageHistoryExpanded = ref(true); // 图片AIGC历史记录是否展开

// 计算属性：按模式分类会话
const textSessions = computed(() => {
  // 确保mode字段正确：如果mode是'image'，不应该出现在textSessions中
  return sessionHistory.value.filter(s => {
    const mode = s.mode || 'text';
    return mode === 'text';
  });
});

const imageSessions = computed(() => {
  // 确保mode字段正确：如果mode是'text'，不应该出现在imageSessions中
  return sessionHistory.value.filter(s => {
    const mode = s.mode || 'text';
    return mode === 'image';
  });
});

// 输入相关
const userInput = ref('');
const uploadedImages = ref([]);
const aigcMode = ref('text'); // 'text' 或 'image'
const isLoading = ref(false);
const abortController = ref(null); // 用于取消请求的AbortController

// UI引用
const sessionListRef = ref(null);
const conversationAreaRef = ref(null);
const previewImageUrl = ref(null);
const previewImageList = ref([]); // 当前预览的图片列表
const previewImageIndex = ref(0); // 当前预览的图片索引
const showComments = ref(false);
const currentResourceId = ref(1); // 默认资源ID，可以根据实际情况修改
const unreadNotifications = ref(0);

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  return date.toLocaleString('zh-CN', { 
    month: '2-digit', 
    day: '2-digit', 
    hour: '2-digit', 
    minute: '2-digit' 
  });
};

// 提取对话主题（不超过20字）- 调用阿里云API
const extractConversationTitle = async (messages) => {
  if (!messages || messages.length === 0) {
    return '新对话';
  }
  
  // 整合用户输入和AI回答
  let conversationText = '';
  messages.forEach(msg => {
    if (msg.role === 'user') {
      conversationText += `用户：${msg.content}\n`;
    } else if (msg.role === 'assistant') {
      conversationText += `AI：${msg.content}\n`;
    }
  });
  
  // 如果对话文本太长，截取前500字（保留更多上下文）
  if (conversationText.length > 500) {
    conversationText = conversationText.substring(0, 500) + '...';
  }
  
  // 调用后端API提取主题（后端会调用阿里云API）
  try {
    const response = await fetch('/api/aigc/extract-title', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ conversation: conversationText })
    });
    
    if (response.ok) {
      const result = await response.json();
      if (result.title && result.title.trim() && result.title !== '新对话') {
        // 确保标题不超过20字
        let title = result.title.trim();
        if (title.length > 20) {
          title = title.substring(0, 20);
        }
        return title;
      }
    } else {
    }
  } catch (error) {
  }
  
  // 降级方案：从对话中提取关键词
  // 优先使用第一条用户消息和第一条AI回答的组合
  const firstUserMsg = messages.find(m => m.role === 'user');
  const firstAIMsg = messages.find(m => m.role === 'assistant');
  
  if (firstUserMsg && firstUserMsg.content) {
    let title = firstUserMsg.content.trim();
    // 移除标点符号和多余空格
    title = title.replace(/[，。！？、；：\s]+/g, ' ').trim();
    
    // 如果AI回答存在且用户消息较短，可以结合AI回答的关键词
    if (firstAIMsg && firstAIMsg.content && title.length < 15) {
      const aiContent = firstAIMsg.content.trim();
      // 提取AI回答中的关键词（前10个字）
      const aiKeywords = aiContent.substring(0, 10).replace(/[，。！？、；：\s]+/g, '');
      if (aiKeywords) {
        title = title + ' ' + aiKeywords;
      }
    }
    
    // 如果超过20字，截取前20字
    if (title.length > 20) {
      title = title.substring(0, 20);
    }
    return title || '新对话';
  }
  
  return '新对话';
};

// 获取会话预览文本
const getSessionPreview = (session) => {
  // 如果会话有消息数量信息，显示消息数量
  if (session.message_count !== undefined) {
    return session.message_count > 0 ? `${session.message_count} 条消息` : '空会话';
  }
  // 否则尝试从当前加载的消息中获取
  if (session.messages && session.messages.length > 0) {
    const firstMsg = session.messages[0];
    return firstMsg.content.substring(0, 30) + (firstMsg.content.length > 30 ? '...' : '');
  }
  return '空会话';
};

// 创建新会话
const createNewSession = async () => {
  // 保存当前会话
  if (currentSessionId.value && currentConversation.value.length > 0) {
    await saveCurrentSession();
  }
  
  const currentUser = getCurrentUser();
  if (!currentUser || !currentUser.id) {
    alert('请先登录');
    return null;
  }
  
  try {
    const response = await fetch('/api/aigc/sessions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': currentUser.id.toString()
      },
      body: JSON.stringify({
        summary: '新对话',
        mode: aigcMode.value  // 传递当前模式
      })
    });
    
    const data = await response.json();
    if (data.success && data.session) {
      const newSession = {
        id: data.session.id,
        title: data.session.summary || '新对话',
        created_at: data.session.created_at,
        mode: data.session.mode || aigcMode.value,  // 保存模式
        messages: []
      };
      sessionHistory.value.unshift(newSession);
      currentSessionId.value = newSession.id;
      currentConversation.value = [];
      userInput.value = '';
      uploadedImages.value = [];
      return newSession;
    } else {
      return null;
    }
  } catch (error) {
    return null;
  }
};

// 加载会话
const loadSession = async (sessionId) => {
  const currentUser = getCurrentUser();
  if (!currentUser || !currentUser.id) {
    alert('请先登录');
    return;
  }
  
  // 检查会话模式是否与当前模式匹配
  const session = sessionHistory.value.find(s => s.id === sessionId);
  if (session && session.mode && session.mode !== aigcMode.value) {
    // 如果模式不匹配，切换模式
    aigcMode.value = session.mode;
  }
  
  try {
    const response = await fetch(`/api/aigc/sessions/${sessionId}/messages?user_id=${currentUser.id}`, {
      method: 'GET',
      headers: {
        'X-User-Id': currentUser.id.toString()
      }
    });
    
    const data = await response.json();
    if (data.success && data.messages) {
      currentSessionId.value = sessionId;
      // 转换消息格式以匹配前端显示（使用新表结构）
      currentConversation.value = data.messages.map(msg => {
        const message = {
          role: msg.role === 'user' ? 'user' : 'assistant',
          content: msg.content || '',
          timestamp: msg.timestamp,
          retrieved_resources: msg.retrieved_resources || null,
          retrieved_resource_ids: msg.retrieved_resource_ids || null,  // 添加检索资源ID列表
          key_entities: msg.key_entities || [],
          sources: msg.sources || '',
          image_path: msg.image_path || null,
          image_paths: msg.image_paths || null,  // 连环画图片路径数组
          is_comic: msg.is_comic || false,  // 是否是连环画
          comic_count: msg.comic_count || 0,  // 连环画数量
          images: msg.images || [],
          model: msg.model || (session.mode || 'text')  // 添加模型类型
        };
        // 确保用户消息有内容显示
        if (message.role === 'user' && !message.content) {
          message.content = '[用户消息]';
        }
        // 如果有retrieved_resource_ids但没有retrieved_resources，尝试加载资源
        if (message.role === 'assistant' && message.retrieved_resource_ids && message.retrieved_resource_ids.length > 0 && !message.retrieved_resources) {
          // 延迟加载检索资源（避免阻塞UI）
          loadRetrievedResourcesForMessage(message);
        }
        return message;
      });
      // 滚动到底部
      await nextTick();
      scrollToBottom();
    } else {
    }
  } catch (error) {
  }
};

// 保存当前会话（每次保存时自动提取主题，调用阿里云API生成）
const saveCurrentSession = async () => {
  if (!currentSessionId.value || currentConversation.value.length === 0) {
    return;
  }
  
  const currentUser = getCurrentUser();
  if (!currentUser || !currentUser.id) {
    return;
  }
  
  // 提取/更新对话主题
  let title = '新对话';
  if (currentConversation.value.length > 0) {
    try {
      // 调用后端API提取主题（后端会调用阿里云API生成）
      title = await extractConversationTitle(currentConversation.value);
      if (!title || !title.trim() || title === '新对话') {
        // 如果提取失败，使用降级方案
        const firstUserMsg = currentConversation.value.find(m => m.role === 'user');
        if (firstUserMsg && firstUserMsg.content) {
          let fallbackTitle = firstUserMsg.content.trim();
          fallbackTitle = fallbackTitle.replace(/[，。！？、；：\s]+/g, ' ').trim();
          if (fallbackTitle.length > 20) {
            fallbackTitle = fallbackTitle.substring(0, 20);
          }
          title = fallbackTitle || '新对话';
        }
      }
    } catch (error) {
      // 提取失败时使用降级方案
      const firstUserMsg = currentConversation.value.find(m => m.role === 'user');
      if (firstUserMsg && firstUserMsg.content) {
        let fallbackTitle = firstUserMsg.content.trim();
        fallbackTitle = fallbackTitle.replace(/[，。！？、；：\s]+/g, ' ').trim();
        if (fallbackTitle.length > 20) {
          fallbackTitle = fallbackTitle.substring(0, 20);
        }
        title = fallbackTitle || '新对话';
      }
    }
  }
  
  // 更新会话摘要到数据库
  try {
    await fetch(`/api/aigc/sessions/${currentSessionId.value}/summary`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': currentUser.id.toString()
      },
      body: JSON.stringify({
        summary: title
      })
    });
    
    // 更新本地会话列表中的标题
    const session = sessionHistory.value.find(s => s.id === currentSessionId.value);
    if (session) {
      session.title = title;
    }
  } catch (error) {
  }
};

// 处理图片上传
const handleImageUpload = (event) => {
  const files = event.target.files;
  if (files && files.length > 0) {
    Array.from(files).forEach(file => {
      if (file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = (e) => {
          uploadedImages.value.push(e.target.result);
        };
        reader.readAsDataURL(file);
      }
    });
  }
};

// 移除图片
const removeImage = (index) => {
  uploadedImages.value.splice(index, 1);
};

// 预览图片（支持图片列表和索引）
const previewImage = (url, imageList = null, index = 0) => {
  previewImageUrl.value = url;
  // 如果提供了图片列表，保存列表和索引用于前后切换
  if (imageList && Array.isArray(imageList) && imageList.length > 0) {
    previewImageList.value = imageList;
    previewImageIndex.value = index;
  } else {
    // 如果没有提供列表，尝试从当前消息中查找
    previewImageList.value = [];
    previewImageIndex.value = 0;
  }
};

// 预览连环画图片（简化模板中的复杂表达式）
const previewComicImage = (imgPath, imagePaths, imgIdx) => {
  // 将图片路径数组转换为完整的URL数组
  const imageList = imagePaths.map(p => getImageUrl(p));
  const currentUrl = getImageUrl(imgPath);
  previewImage(currentUrl, imageList, imgIdx);
};

// 切换到上一张图片
const prevImage = () => {
  if (previewImageList.value.length > 0 && previewImageIndex.value > 0) {
    previewImageIndex.value--;
    previewImageUrl.value = previewImageList.value[previewImageIndex.value];
  }
};

// 切换到下一张图片
const nextImage = () => {
  if (previewImageList.value.length > 0 && previewImageIndex.value < previewImageList.value.length - 1) {
    previewImageIndex.value++;
    previewImageUrl.value = previewImageList.value[previewImageIndex.value];
  }
};

// 键盘事件处理（支持左右键切换图片）
const handleKeydown = (event) => {
  if (previewImageUrl.value && previewImageList.value.length > 0) {
    if (event.key === 'ArrowLeft') {
      event.preventDefault();
      prevImage();
    } else if (event.key === 'ArrowRight') {
      event.preventDefault();
      nextImage();
    } else if (event.key === 'Escape') {
      event.preventDefault();
      previewImageUrl.value = null;
      previewImageList.value = [];
      previewImageIndex.value = 0;
    }
  }
};

// 在组件挂载时添加键盘事件监听
onMounted(() => {
  window.addEventListener('keydown', handleKeydown);
});

// 在组件卸载时移除键盘事件监听
onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown);
});

// 发送消息（支持流式输出）
const sendMessage = async () => {
  if (isLoading.value) return;
  
  // 验证输入：文本模式和图片模式都需要有内容
  const inputText = userInput.value.trim();
  if (aigcMode.value === 'text' && !inputText) {
    alert('请输入您的问题');
    return;
  }
  if (aigcMode.value === 'image' && !inputText && uploadedImages.value.length === 0) {
    alert('请输入图片生成提示词或上传参考图片');
    return;
  }

  // 检查当前会话的模式是否与当前模式匹配
  if (currentSessionId.value) {
    const currentSession = sessionHistory.value.find(s => s.id === currentSessionId.value);
    if (currentSession && currentSession.mode && currentSession.mode !== aigcMode.value) {
      // 如果模式不匹配，提示用户并创建新会话
      if (!confirm(`当前会话是${currentSession.mode === 'text' ? '文字' : '图片'}AIGC模式，您正在使用${aigcMode.value === 'text' ? '文字' : '图片'}AIGC模式。是否创建新会话？`)) {
        return;
      }
      // 保存当前会话
      await saveCurrentSession();
      // 创建新会话
      const newSession = await createNewSession();
      if (!newSession) {
        alert('创建会话失败，请稍后重试');
        return;
      }
    }
  } else {
    // 如果没有当前会话，创建新会话
    const newSession = await createNewSession();
    if (!newSession) {
      alert('创建会话失败，请稍后重试');
      return;
    }
  }

  const userMessage = {
    role: 'user',
    content: userInput.value.trim(),
    images: aigcMode.value === 'image' ? [...uploadedImages.value] : [],
    mode: aigcMode.value,
    timestamp: new Date().toISOString()
  };

  currentConversation.value.push(userMessage);
  
  // 注意：用户消息和AI消息将一起保存，所以这里先不保存
  
  // inputText已在上面声明，这里直接使用
  userInput.value = '';
  uploadedImages.value = [];
  isLoading.value = true;

  // 创建AbortController用于取消请求
  abortController.value = new AbortController();

  // 创建AI消息占位符（用于流式更新）
  const aiMessage = {
    role: 'assistant',
    content: '',
    retrieved_resources: null,
    key_entities: [],
    sources: '',
    image_path: null,
    model: aigcMode.value,  // 设置模型类型，用于显示AI昵称
    timestamp: new Date().toISOString()
  };
  currentConversation.value.push(aiMessage);

  // 滚动到底部
  await nextTick();
  scrollToBottom();

  try {
    // 获取当前用户信息
    const currentUser = getCurrentUser();
    if (!currentUser || !currentUser.id) {
      throw new Error('用户未登录，请先登录');
    }

    // 调用后端API（普通模式，不使用流式输出）
    const formData = new FormData();
    formData.append('query', inputText);
    formData.append('mode', aigcMode.value);
    formData.append('stream', 'false');  // 禁用流式输出
    
    // 添加session_id（如果存在）
    if (currentSessionId.value) {
      formData.append('session_id', currentSessionId.value.toString());
    }
    
    if (aigcMode.value === 'image' && userMessage.images.length > 0) {
      userMessage.images.forEach((img, idx) => {
        const blob = dataURLtoBlob(img);
        formData.append(`images`, blob, `image_${idx}.jpg`);
      });
    }

    // 使用fetch处理响应，支持取消
    const response = await fetch('/api/aigc/chat', {
      method: 'POST',
      headers: {
        'X-User-Id': currentUser.id.toString()  // 在请求头中添加用户ID
      },
      body: formData,
      signal: abortController.value.signal  // 添加signal以支持取消
    });

    if (!response.ok) {
      // 尝试读取错误响应
      let errorMessage = `HTTP ${response.status}: ${response.statusText}`;
      try {
        // 对于流式响应，需要先读取响应体
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
          const errorData = await response.json();
          if (errorData.error || errorData.answer) {
            errorMessage = errorData.answer || errorData.error || errorMessage;
          }
        } else {
          // 对于非JSON响应，尝试读取文本
          const text = await response.text();
          if (text) {
            try {
              const errorData = JSON.parse(text);
              errorMessage = errorData.answer || errorData.error || errorMessage;
            } catch {
              errorMessage = text.substring(0, 200) || errorMessage;
            }
          }
        }
      } catch (e) {
        // 使用默认错误信息
      }
      throw new Error(errorMessage);
    }

    // 处理JSON响应（非流式）
    try {
      const data = await response.json();
      
      if (data.error) {
        // 如果有错误，显示错误消息
        aiMessage.content = data.answer || data.error || '处理失败';
        // 如果有默认图片，也显示出来
        if (data.image_path) {
          aiMessage.image_path = data.image_path;
        }
        // 不要抛出错误，让用户看到错误消息
        currentConversation.value.push(aiMessage);
        return;
      } else {
        aiMessage.content = data.answer || '处理成功';
        
        // 处理图片路径（图片AIGC模式）
        // 检查是否是连环画
        if (data.is_comic && data.image_paths && Array.isArray(data.image_paths)) {
          // 连环画：使用image_paths数组
          aiMessage.image_paths = data.image_paths;
          aiMessage.is_comic = true;
          aiMessage.comic_count = data.comic_count || data.image_paths.length;
          // 第一张图片作为主图（用于兼容旧代码）
          aiMessage.image_path = data.image_path || data.image_paths[0];
        } else if (data.image_path) {
          // 单张图片
          aiMessage.image_path = data.image_path;
          aiMessage.is_comic = false;
        }
        
        // 设置模型类型（用于显示AI昵称）
        // 优先使用后端返回的model，如果没有则使用当前模式
        aiMessage.model = data.model || aigcMode.value;  // 'text' 或 'image'
        
        // 处理其他字段
        if (data.key_entities) {
          aiMessage.key_entities = data.key_entities;
        }
        if (data.sources) {
          aiMessage.sources = data.sources;
        }
        if (data.retrieved_resources) {
          aiMessage.retrieved_resources = data.retrieved_resources;
        }
        if (data.retrieved_resource_ids) {
          aiMessage.retrieved_resource_ids = data.retrieved_resource_ids;
        }
      }
      
      // 注意：消息已在后端AIGC chat接口中自动保存，这里不需要再次保存
    } catch (e) {
      aiMessage.content = '解析响应失败，请稍后重试';
    }

    // 确保内容不为空
    if (!aiMessage.content) {
      aiMessage.content = '抱歉，未能生成有效回答。';
    }

    await saveCurrentSession();
  } catch (error) {
    // 检查是否是用户主动取消
    if (error.name === 'AbortError') {
      aiMessage.content = '生成已取消';
      // 移除AI消息占位符（因为已取消）
      const index = currentConversation.value.indexOf(aiMessage);
      if (index > -1) {
        currentConversation.value.splice(index, 1);
      }
      return; // 取消时不保存会话
    }
    
    // 其他错误处理
    const errorMessage = error.message || '未知错误';
    let errorContent = `抱歉，生成失败：${errorMessage}。`;
    
    // 提供更友好的错误提示
    if (errorMessage.includes('Failed to fetch') || errorMessage.includes('NetworkError')) {
      errorContent += '\n\n请确保：\n1. 后端API服务器已启动（运行 python aigc_api_server.py）\n2. 后端服务正常运行（查看终端输出）\n3. 检查网络连接';
    } else if (errorMessage.includes('未初始化') || errorMessage.includes('未配置')) {
      errorContent += '\n\n请检查：\n1. API密钥是否正确配置在.env文件中\n2. 数据库连接是否正常';
    }
    
    aiMessage.content = errorContent;
    await saveCurrentSession();
  } finally {
    isLoading.value = false;
    abortController.value = null; // 清除AbortController
    await nextTick();
    scrollToBottom();
  }
};

// 取消生成
const cancelGeneration = () => {
  if (abortController.value) {
    abortController.value.abort();
  }
};

// Base64转Blob
const dataURLtoBlob = (dataURL) => {
  const arr = dataURL.split(',');
  const mime = arr[0].match(/:(.*?);/)[1];
  const bstr = atob(arr[1]);
  let n = bstr.length;
  const u8arr = new Uint8Array(n);
  while (n--) {
    u8arr[n] = bstr.charCodeAt(n);
  }
  return new Blob([u8arr], { type: mime });
};

// 滚动到底部
const scrollToBottom = () => {
  if (conversationAreaRef.value) {
    conversationAreaRef.value.scrollTop = conversationAreaRef.value.scrollHeight;
  }
};

// 检索资源展开/折叠状态（按消息ID存储）
const resourceExpandedStates = ref({});

// 切换检索资源展开/折叠状态
const toggleResourceExpanded = (msg) => {
  const msgId = msg.id || msg.timestamp || JSON.stringify(msg);
  resourceExpandedStates.value[msgId] = !resourceExpandedStates.value[msgId];
};

// 检查检索资源是否展开
const isResourceExpanded = (msg) => {
  const msgId = msg.id || msg.timestamp || JSON.stringify(msg);
  // 默认展开（如果没有设置过状态）
  return resourceExpandedStates.value[msgId] !== false;
};

// 为消息加载检索资源（如果有retrieved_resource_ids但没有retrieved_resources）
const loadRetrievedResourcesForMessage = async (message) => {
  if (!message.retrieved_resource_ids || message.retrieved_resource_ids.length === 0) {
    return;
  }
  
  try {
    // 从后端加载资源详情
    const resourceIds = message.retrieved_resource_ids.join(',');
    const response = await fetch(`/api/aigc/resources?ids=${resourceIds}`);
    const data = await response.json();
    
    if (data.success && data.resources) {
      // 构建retrieved_resources结构
      message.retrieved_resources = {
        database_results: data.resources.map(resource => ({
          id: resource.id,
          title: resource.title || resource.entity_name || '未命名资源',
          content: resource.description || resource.content || '',
          table: resource.table || 'cultural_resources',
          source: resource.source || ''
        }))
      };
    }
  } catch (error) {
  }
};

// 从检索结果跳转到资源详情
const goToResourceDetailFromRetrieved = (item) => {
  // 根据资源类型和ID跳转到资源详情页
  if (item.id && item.table) {
    // 构建跳转参数
    const query = {
      resource_id: item.id,
      resource_type: item.table,
      entity_name: item.title || ''
    };
    
    // 跳转到资源详情页
    router.push({
      path: '/resource/detail',
      query: query
    });
  } else {
  }
};

// 切换评论面板
const toggleComments = () => {
  showComments.value = !showComments.value;
  if (showComments.value) {
    loadNotifications();
  }
};

// 加载未读通知
const loadNotifications = async () => {
  try {
    const userInfo = getCurrentUser();
    if (!userInfo || !userInfo.id) return;
    
    const response = await fetch(`/api/notifications?user_id=${userInfo.id}&is_read=0`);
    const data = await response.json();
    
    if (data.success) {
      unreadNotifications.value = data.notifications?.length || 0;
    }
  } catch (error) {
  }
};

// 格式化答案文本（支持换行等）
const formatAnswerText = (text) => {
  if (!text) return '';
  // 将换行符转换为HTML换行
  return text.replace(/\n/g, '<br>');
};

// 获取资源图片URL
const getResourceImageUrl = (storagePath, tableName) => {
  if (!storagePath) return '';
  
  // 根据表名确定文件夹
  let folder = '';
  if (tableName === 'AIGC_graph') {
    folder = '/AIGC_graph/';
  } else if (tableName === 'crawled_images') {
    folder = '/crawled_images/';
  } else {
    folder = '/images/';
  }
  
  // 如果storagePath已经是完整路径，直接返回
  if (storagePath.startsWith('http://') || storagePath.startsWith('https://')) {
    return storagePath;
  }
  
  // 否则拼接路径
  return folder + storagePath.replace(/^\/+/, '');
};

// 获取图片URL（用于AIGC生成的图片和用户上传的图片）
// 参考头像显示方法：如果以 / 开头，直接返回（已经是正确的路径格式）
const getImageUrl = (imagePath) => {
  if (!imagePath) return '';
  
  // 如果已经是完整URL，直接返回
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    return imagePath;
  }
  
  // 处理绝对路径（Windows路径，如：D:\git\mygit\Java-project\AIGC_graph\0001.jpeg）
  // 或包含绝对路径的相对路径（如：/AIGC_graph/D:\git\mygit\Java-project\AIGC_graph\0001.jpeg）
  if (imagePath.includes(':\\') || imagePath.includes(':/')) {
    // 提取文件名
    const parts = imagePath.split(/[/\\]/);
    const filename = parts[parts.length - 1];
    // 如果文件名存在，根据路径判断是AIGC_graph还是image_from_users
    if (filename) {
      if (imagePath.includes('AIGC_graph')) {
        return `/AIGC_graph/${filename}`;
      } else if (imagePath.includes('image_from_users')) {
        return `/image_from_users/${filename}`;
      } else {
        return `/AIGC_graph/${filename}`;  // 默认
      }
    }
  }
  
  // 如果以 / 开头，直接返回（已经是正确的路径格式，参考头像显示方法）
  if (imagePath.startsWith('/')) {
    // 如果路径中包含Windows绝对路径特征，提取文件名
    if (imagePath.includes(':\\') || imagePath.includes(':/')) {
      const parts = imagePath.split(/[/\\]/);
      const filename = parts[parts.length - 1];
      if (filename) {
        // 根据路径判断文件夹
        if (imagePath.includes('AIGC_graph')) {
          return `/AIGC_graph/${filename}`;
        } else if (imagePath.includes('image_from_users')) {
          return `/image_from_users/${filename}`;
        } else {
          return `/AIGC_graph/${filename}`;  // 默认
        }
      }
    }
    // 否则直接返回（已经是正确的相对路径格式，如：/AIGC_graph/0001.jpeg 或 /image_from_users/xxx.jpg）
    return imagePath;
  }
  
  // 如果以 ./ 开头，转换为 / 开头
  if (imagePath.startsWith('./')) {
    return imagePath.replace('./', '/');
  }
  
  // 其他情况，添加 / 前缀
  return '/' + imagePath;
};

// 从数据库加载会话列表
const loadSessionsFromDB = async () => {
  const currentUser = getCurrentUser();
  if (!currentUser || !currentUser.id) {
    return;
  }
  
  try {
    const response = await fetch(`/api/aigc/sessions?user_id=${currentUser.id}`, {
      method: 'GET',
      headers: {
        'X-User-Id': currentUser.id.toString()
      }
    });
    
    const data = await response.json();
    if (data.success && data.sessions) {
      sessionHistory.value = data.sessions.map(session => ({
        id: session.id,
        title: session.summary || '新对话',
        created_at: session.created_at,
        mode: session.mode || 'text',  // 保存模式，确保从数据库正确读取
        message_count: session.message_count || 0
      }));
      // 调试：打印会话模式
    } else {
      sessionHistory.value = [];
    }
  } catch (error) {
    sessionHistory.value = [];
  }
};

// 切换历史记录面板显示/隐藏
const toggleHistoryPanel = () => {
  isHistoryCollapsed.value = !isHistoryCollapsed.value;
  // 保存状态到本地存储
  localStorage.setItem('aigc_history_collapsed', isHistoryCollapsed.value.toString());
};

// 处理会话点击（区分复选框和内容区域）
const handleSessionClick = (sessionId, event) => {
  // 如果点击的是复选框区域，不加载会话
  if (event.target.classList.contains('session-checkbox') || event.target.closest('.session-checkbox')) {
    return;
  }
  loadSession(sessionId);
};

// 切换会话选择状态
const toggleSessionSelection = (sessionId) => {
  const index = selectedSessions.value.indexOf(sessionId);
  if (index > -1) {
    selectedSessions.value.splice(index, 1);
  } else {
    selectedSessions.value.push(sessionId);
  }
};

// 全选/取消全选
const isAllSelected = computed(() => {
  const allSessions = [...textSessions.value, ...imageSessions.value];
  return allSessions.length > 0 && allSessions.every(s => selectedSessions.value.includes(s.id));
});

const toggleSelectAll = () => {
  const allSessions = [...textSessions.value, ...imageSessions.value];
  if (isAllSelected.value) {
    // 取消全选
    selectedSessions.value = [];
  } else {
    // 全选
    selectedSessions.value = allSessions.map(s => s.id);
  }
};

// 取消所有选中
const clearSelection = () => {
  selectedSessions.value = [];
};

// 删除选中的会话
const deleteSelectedSessions = async () => {
  if (selectedSessions.value.length === 0) {
    return;
  }
  
  if (!confirm(`确定要删除选中的 ${selectedSessions.value.length} 个会话吗？此操作不可恢复。`)) {
    return;
  }
  
  const currentUser = getCurrentUser();
  if (!currentUser || !currentUser.id) {
    alert('请先登录');
    return;
  }
  
  try {
    const response = await fetch('/api/aigc/sessions/batch', {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        'X-User-Id': currentUser.id.toString()
      },
      body: JSON.stringify({
        session_ids: selectedSessions.value
      })
    });
    
    const data = await response.json();
    if (data.success) {
      // 如果当前会话被删除，清空对话
      if (selectedSessions.value.includes(currentSessionId.value)) {
        currentSessionId.value = null;
        currentConversation.value = [];
      }
      
      // 从列表中移除已删除的会话
      const deletedIds = [...selectedSessions.value];
      sessionHistory.value = sessionHistory.value.filter(
        s => !deletedIds.includes(s.id)
      );
      
      // 如果当前会话被删除，需要清空对话
      if (deletedIds.includes(currentSessionId.value)) {
        currentSessionId.value = null;
        currentConversation.value = [];
      }
      
      // 清空选中列表
      selectedSessions.value = [];
      
      alert('删除成功');
    } else {
      alert('删除失败：' + (data.message || '未知错误'));
    }
  } catch (error) {
    alert('删除失败，请稍后重试');
  }
};

// 删除所有会话
const deleteAllSessions = async () => {
  if (sessionHistory.value.length === 0) {
    return;
  }
  
  if (!confirm(`确定要删除所有 ${sessionHistory.value.length} 个会话吗？此操作不可恢复。`)) {
    return;
  }
  
  const currentUser = getCurrentUser();
  if (!currentUser || !currentUser.id) {
    alert('请先登录');
    return;
  }
  
  try {
    const response = await fetch('/api/aigc/sessions/all', {
      method: 'DELETE',
      headers: {
        'X-User-Id': currentUser.id.toString()
      }
    });
    
    const data = await response.json();
    if (data.success) {
      // 清空当前会话和对话
      currentSessionId.value = null;
      currentConversation.value = [];
      sessionHistory.value = [];
      selectedSessions.value = [];
      
      alert('删除成功');
    } else {
      alert('删除失败：' + (data.message || '未知错误'));
    }
  } catch (error) {
    alert('删除失败，请稍后重试');
  }
};

onMounted(async () => {
  // 从本地存储恢复历史记录面板状态
  const savedCollapsedState = localStorage.getItem('aigc_history_collapsed');
  if (savedCollapsedState !== null) {
    isHistoryCollapsed.value = savedCollapsedState === 'true';
  }
  
  // 加载未读通知
  loadNotifications();
  
  // 从数据库加载历史会话
  await loadSessionsFromDB();
  
  // 从URL参数中恢复对话（如果有）
  const urlParams = new URLSearchParams(window.location.search);
  const conversationParam = urlParams.get('conversation');
  if (conversationParam) {
    try {
      const messages = JSON.parse(decodeURIComponent(conversationParam));
      const newSession = await createNewSession();
      if (newSession) {
        // 将消息保存到数据库
        for (const msg of messages) {
          await fetch(`/api/aigc/sessions/${newSession.id}/messages`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'X-User-Id': getCurrentUser().id.toString()
            },
            body: JSON.stringify({
              sender: msg.role === 'user' ? 'user' : 'ai',
              content: msg.content || ''
            })
          });
        }
        currentConversation.value = messages;
      }
    } catch (e) {
    }
  }
});
</script>

<style scoped>
.aigc-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px);
  background: #f5f7fa;
}

.forum-header {
  display: flex;
  justify-content: flex-end;
  padding: 12px 30px;
  background: white;
  border-bottom: 1px solid #eee;
}

.forum-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.forum-btn:hover {
  background: #66b1ff;
}

.forum-icon {
  font-size: 16px;
}

.aigc-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* 左侧历史会话导航 */
.session-nav-panel {
  width: 220px;
  background: white;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
}

.session-nav-panel.collapsed {
  width: 50px;
}

.session-nav-panel.collapsed .session-nav-header h3,
.session-nav-panel.collapsed .delete-actions,
.session-nav-panel.collapsed .session-list {
  display: none;
}

.session-nav-panel.collapsed .header-actions {
  justify-content: center;
  width: 100%;
}

.session-nav-header {
  padding: 15px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.toggle-history-btn {
  width: 28px;
  height: 28px;
  background: #f0f2f5;
  color: #666;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
  padding: 0;
}

.toggle-history-btn:hover {
  background: #e4e7ed;
  color: #409eff;
}

.session-nav-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
  font-weight: 500;
}

.new-chat-btn {
  width: 28px;
  height: 28px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.3s;
  padding: 0;
}

.new-chat-btn:hover {
  background: #66b1ff;
}

.new-chat-btn span {
  font-weight: 300;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.session-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  background: #f9f9f9;
  border: 1px solid transparent;
}

.session-item:hover {
  background: #f0f0f0;
}

.session-item.active {
  background: #e3f2fd;
  border-color: #409eff;
}

.session-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  font-size: 11px;
  color: #999;
  margin-bottom: 4px;
}

.session-preview {
  font-size: 12px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-sessions {
  text-align: center;
  color: #999;
  padding: 20px;
  font-size: 13px;
}

.history-section {
  margin-bottom: 16px;
}

.history-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  transition: background 0.3s;
}

.history-section-header:hover {
  background: #e4e7ed;
}

.expand-icon {
  font-size: 12px;
  color: #666;
}

.history-section-content {
  margin-top: 8px;
}

/* 删除操作栏 */
.delete-actions {
  padding: 10px;
  border-bottom: 1px solid #eee;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.select-all-btn,
.clear-selection-btn,
.delete-btn,
.delete-all-btn {
  flex: 1;
  padding: 6px 12px;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.3s;
  min-width: 80px;
}

.select-all-btn {
  background: #67c23a;
}

.select-all-btn:hover {
  background: #85ce61;
}

.clear-selection-btn {
  background: #909399;
}

.clear-selection-btn:hover {
  background: #a6a9ad;
}

.delete-btn {
  background: #f56c6c;
}

.delete-btn:hover:not(:disabled) {
  background: #f78989;
}

.delete-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.delete-all-btn {
  background: #e6a23c;
}

.delete-all-btn:hover {
  background: #ebb563;
}

.session-item.selected {
  background: #fff3e0;
  border-color: #ff9800;
}

.session-checkbox {
  margin-top: 2px;
  cursor: pointer;
  flex-shrink: 0;
}

.session-content {
  flex: 1;
  min-width: 0;
}

/* 右侧主面板 */
.main-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  overflow: hidden;
}

/* 对话显示区域 */
.conversation-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px 30px;
  background: #fafafa;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.message-item.user {
  flex-direction: row-reverse;  /* 用户消息在右边 */
  justify-content: flex-start;   /* 左对齐 */
}

.message-item.assistant {
  flex-direction: row;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  overflow: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-item.user .message-avatar {
  background: #409eff;
}

.message-item.assistant .message-avatar {
  background: #67c23a;
}

.message-content-wrapper {
  flex: 1;
  max-width: calc(100% - 48px);
  display: flex;
  flex-direction: column;
}

.message-item.user .message-content-wrapper {
  max-width: 70%;  /* 用户消息宽度为70%，不要太窄 */
  align-items: flex-end;  /* 右对齐 */
  margin-left: auto;  /* 靠右显示 */
}

.message-item.assistant .message-content-wrapper {
  max-width: 75%;  /* AI回答最多占3/4屏幕宽度，自适应 */
  align-items: flex-start;  /* 左对齐 */
}

.message-role-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.message-item.user .message-role-label {
  text-align: left;  /* 用户消息的标签左对齐（因为消息框本身靠右） */
  width: 100%;
  align-self: flex-start;  /* 确保标签左对齐 */
}

.message-content {
  background: white;
  padding: 12px 16px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.message-item.user .message-content {
  background: #e3f2fd;
  text-align: left;  /* 内容左对齐 */
}

.message-text {
  font-size: 14px;
  color: #333;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

/* AI回答左右分栏布局 */
.ai-response-layout {
  display: flex;
  gap: 16px;
  width: 100%;
  min-height: 300px;
}

.resources-panel,
.answer-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  overflow: hidden;
}

.panel-header {
  padding: 12px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.resources-content,
.answer-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  max-height: 75vh;  /* 最多占3/4屏幕高度，自适应 */
}

.resource-section {
  margin-bottom: 20px;
}

.resource-section:last-child {
  margin-bottom: 0;
}

.resource-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 12px;
  padding-bottom: 6px;
  border-bottom: 1px solid #e4e7ed;
}

.resource-item {
  padding: 12px;
  margin-bottom: 12px;
  background: #f9f9f9;
  border-radius: 6px;
  border-left: 3px solid #409eff;
}

.resource-item:last-child {
  margin-bottom: 0;
}

.resource-source {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.resource-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.resource-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 8px;
  word-break: break-word;
}

.resource-source-url {
  font-size: 12px;
  color: #409eff;
  margin-top: 8px;
}

.resource-source-url a {
  color: #409eff;
  text-decoration: none;
}

.resource-source-url a:hover {
  text-decoration: underline;
}

/* 折叠状态的资源列表（支持滑动） */
.resources-collapsed {
  padding: 8px;
  background: #f9f9f9;
  border-radius: 6px;
  margin-top: 8px;
}

.collapsed-resources-scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 4px 0;
  scrollbar-width: thin;
  scrollbar-color: #c0c4cc #f5f7fa;
}

.collapsed-resources-scroll::-webkit-scrollbar {
  height: 6px;
}

.collapsed-resources-scroll::-webkit-scrollbar-track {
  background: #f5f7fa;
  border-radius: 3px;
}

.collapsed-resources-scroll::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
}

.collapsed-resources-scroll::-webkit-scrollbar-thumb:hover {
  background: #a0a4a8;
}

.collapsed-resource-item {
  flex-shrink: 0;
  padding: 8px 12px;
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  min-width: 120px;
  max-width: 200px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.collapsed-resource-item:hover {
  background: #f0f9ff;
  border-color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.collapsed-resource-name {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.collapsed-resource-type {
  font-size: 11px;
  color: #909399;
}

.resource-image {
  margin-top: 10px;
}

.resource-img {
  max-width: 100%;
  max-height: 200px;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid #ddd;
}

.no-resources {
  text-align: center;
  color: #909399;
  padding: 40px 20px;
  font-size: 13px;
}

.answer-content .message-text {
  background: transparent;
  padding: 0;
  box-shadow: none;
}

.key-entities {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
}

.entities-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.entities-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.entity-tag {
  display: inline-block;
  padding: 4px 10px;
  background: #e3f2fd;
  color: #409eff;
  border-radius: 12px;
  font-size: 12px;
}

.sources-info {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
}

.sources-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.sources-text {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.message-images {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.message-image {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid #ddd;
}

.message-image-result {
  margin-top: 10px;
}

.result-image {
  max-width: 100%;
  max-height: 400px;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid #ddd;
}

.message-comic-result {
  margin-top: 10px;
}

.comic-header {
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 10px;
}

.comic-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 10px;
  margin-top: 10px;
}

.comic-image {
  width: 100%;
  max-width: 100%;
  height: auto;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid #ddd;
  transition: transform 0.2s;
}

.comic-image:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
  text-align: right;
}

.empty-conversation {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-text {
  font-size: 14px;
}

/* 输入区域（页面中部） */
.input-area {
  padding: 20px 30px;
  background: white;
  border-top: 1px solid #eee;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mode-switch {
  display: flex;
  gap: 8px;
}

.mode-btn {
  padding: 8px 16px;
  background: #f0f2f5;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.3s;
}

.mode-btn:hover {
  background: #e4e7ed;
}

.mode-btn.active {
  background: #409eff;
  color: white;
  border-color: #409eff;
  font-weight: 500;
}

.secondary-creation-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.secondary-creation-btn:hover {
  background: linear-gradient(135deg, #764ba2 0%, #667eea 100%);
}

.upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #f0f2f5;
  border: 1px dashed #ddd;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.3s;
}

.upload-btn:hover {
  background: #e4e7ed;
  border-color: #409eff;
}

.upload-icon {
  font-size: 16px;
}

.uploaded-images {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.uploaded-image-item {
  position: relative;
  width: 80px;
  height: 80px;
}

.preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #ddd;
}

.remove-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 22px;
  height: 22px;
  background: #f56c6c;
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.text-input-section {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.text-input {
  flex: 1;
  min-height: 100px;
  max-height: 200px;
  padding: 12px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  outline: none;
  transition: border-color 0.3s;
}

.text-input:focus {
  border-color: #409eff;
}

.send-btn {
  padding: 12px 28px;
  background: #409eff;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: background 0.3s;
  white-space: nowrap;
  height: fit-content;
}

.send-btn:hover:not(:disabled) {
  background: #66b1ff;
}

.send-btn:disabled {
  background: #c0c4cc;
  cursor: not-allowed;
}

.stop-btn {
  background: #f56c6c !important;
}

.stop-btn:hover {
  background: #f78989 !important;
}

/* 图片预览模态框 */
.image-preview-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  cursor: pointer;
}

.preview-modal-image {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 8px;
  cursor: default;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
}

.preview-close-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 40px;
  height: 40px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  border-radius: 50%;
  font-size: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1001;
  transition: background 0.3s;
}

.preview-close-btn:hover {
  background: rgba(0, 0, 0, 0.8);
}

.preview-nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 50px;
  height: 50px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  border-radius: 50%;
  font-size: 32px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1001;
  transition: background 0.3s;
  user-select: none;
}

.preview-nav-btn:hover {
  background: rgba(0, 0, 0, 0.8);
}

.preview-prev-btn {
  left: 20px;
}

.preview-next-btn {
  right: 20px;
}

.preview-indicator {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.6);
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  z-index: 1001;
}

/* 滚动条样式 */
.session-list::-webkit-scrollbar,
.conversation-area::-webkit-scrollbar {
  width: 6px;
}

.session-list::-webkit-scrollbar-track,
.conversation-area::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.session-list::-webkit-scrollbar-thumb,
.conversation-area::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.session-list::-webkit-scrollbar-thumb:hover,
.conversation-area::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 评论面板样式 */
.comments-panel {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 600px;
  max-width: 90vw;
  max-height: 80vh;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.3);
  z-index: 2000;
  overflow: hidden;
}

.notification-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #f56c6c;
  color: #fff;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
