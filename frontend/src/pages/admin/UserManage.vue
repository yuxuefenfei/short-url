<template>
  <div class="user-manage">
    <AdminPageHeader
      title="用户管理"
      subtitle="管理系统用户和权限"
    />

    <!-- 搜索和过滤 -->
    <a-card class="filter-card">
      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :sm="8" :md="6">
          <a-input-search
            v-model:value="searchKeyword"
            placeholder="搜索用户名或邮箱"
            enter-button="搜索"
            @search="handleSearch"
            allow-clear
          />
        </a-col>
        <a-col :xs="12" :sm="6" :md="4">
          <a-select
            v-model:value="roleFilter"
            placeholder="角色筛选"
            style="width: 100%"
            @change="handleFilter"
            allow-clear
          >
            <a-select-option value="USER">普通用户</a-select-option>
            <a-select-option value="ADMIN">管理员</a-select-option>
          </a-select>
        </a-col>
        <a-col :xs="12" :sm="6" :md="4">
          <a-select
            v-model:value="statusFilter"
            placeholder="状态筛选"
            style="width: 100%"
            @change="handleFilter"
            allow-clear
          >
            <a-select-option value="1">正常</a-select-option>
            <a-select-option value="0">已禁用</a-select-option>
          </a-select>
        </a-col>
        <a-col :xs="24" :sm="4" :md="10" class="action-buttons">
          <a-button type="primary" @click="showCreateModal = true">
            <PlusOutlined />
            添加用户
          </a-button>
          <a-button @click="handleExport" style="margin-left: 8px;">
            <ExportOutlined />
            导出数据
          </a-button>
          <a-button @click="handleRefresh" style="margin-left: 8px;">
            <ReloadOutlined />
            刷新
          </a-button>
        </a-col>
      </a-row>
    </a-card>

    <!-- 数据表格 -->
    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="userList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        :scroll="{ x: 1000 }"
      >
        <!-- 用户名列 -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'username'">
            <div class="user-cell">
              <a-avatar :style="{ backgroundColor: getAvatarColor(record.username) }">
                {{ record.username.charAt(0).toUpperCase() }}
              </a-avatar>
              <div class="user-info">
                <div class="username">{{ record.username }}</div>
                <div class="email" v-if="record.email">{{ record.email }}</div>
              </div>
            </div>
          </template>

          <!-- 角色列 -->
          <template v-else-if="column.key === 'role'">
            <a-tag :color="record.role === 'ADMIN' ? 'gold' : 'blue'">
              {{ record.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </a-tag>
          </template>

          <!-- 状态列 -->
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'success' : 'error'">
              {{ record.status === 1 ? '正常' : '禁用' }}
            </a-tag>
          </template>

          <!-- 最后登录列 -->
          <template v-else-if="column.key === 'lastLoginTime'">
            <div class="time-cell">
              <div>{{ record.lastLoginTime ? formatDateTime(record.lastLoginTime) : '从未登录' }}</div>
              <div class="time-ago" v-if="record.lastLoginTime">
                {{ formatTimeAgo(record.lastLoginTime) }}
              </div>
            </div>
          </template>

          <!-- 创建日期列 -->
          <template v-else-if="column.key === 'createdTime'">
            <div class="time-cell">
              <div>{{ formatDateTime(record.createdTime) }}</div>
              <div class="time-ago">{{ formatTimeAgo(record.createdTime) }}</div>
            </div>
          </template>

          <!-- 操作列 -->
          <template v-else-if="column.key === 'action'">
            <div class="action-cell">
              <a-space>
                <a @click="viewDetails(record)">详情</a>
                <a-divider type="vertical" />
                <a @click="editUser(record)">编辑</a>
                <a-divider type="vertical" />
                <a @click="toggleStatus(record)">
                  {{ record.status === 1 ? '禁用' : '启用' }}
                </a>
                <a-divider type="vertical" />
                <a @click="resetPassword(record)" style="color: #fa8c16;">重置密码</a>
                <a-divider type="vertical" />
                <a @click="deleteUser(record)" style="color: #ff4d4f;">删除</a>
              </a-space>
            </div>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 添加用户模态框 -->
    <a-modal
      v-model:visible="showCreateModal"
      title="添加用户"
      @ok="handleCreateUser"
      @cancel="handleCancelCreate"
      :confirm-loading="creating"
    >
      <a-form
        :model="createForm"
        :rules="createRules"
        ref="createFormRef"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 20 }"
      >
        <a-form-item label="用户名" name="username" required>
          <a-input
            v-model:value="createForm.username"
            placeholder="请输入用户名"
          />
        </a-form-item>
        <a-form-item label="密码" name="password" required>
          <a-input-password
            v-model:value="createForm.password"
            placeholder="请输入密码"
          />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input
            v-model:value="createForm.email"
            placeholder="请输入邮箱（可选）"
          />
        </a-form-item>
        <a-form-item label="角色" name="role" required>
          <a-select v-model:value="createForm.role" placeholder="请选择角色">
            <a-select-option value="USER">普通用户</a-select-option>
            <a-select-option value="ADMIN">管理员</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 编辑用户模态框 -->
    <a-modal
      v-model:visible="showEditModal"
      title="编辑用户"
      @ok="handleEditUser"
      @cancel="handleCancelEdit"
      :confirm-loading="editing"
    >
      <a-form
        :model="editForm"
        :rules="editRules"
        ref="editFormRef"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 20 }"
      >
        <a-form-item label="用户名" name="username" required>
          <a-input
            v-model:value="editForm.username"
            placeholder="请输入用户名"
          />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input
            v-model:value="editForm.email"
            placeholder="请输入邮箱（可选）"
          />
        </a-form-item>
        <a-form-item label="角色" name="role" required>
          <a-select v-model:value="editForm.role" placeholder="请选择角色">
            <a-select-option value="USER">普通用户</a-select-option>
            <a-select-option value="ADMIN">管理员</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 用户详情模态框 -->
    <a-modal
      v-model:visible="showDetailModal"
      title="用户详情"
      :footer="null"
      width="600px"
    >
      <div v-if="selectedUser" class="detail-content">
        <a-descriptions bordered :column="1">
          <a-descriptions-item label="用户ID">
            {{ selectedUser.id }}
          </a-descriptions-item>
          <a-descriptions-item label="用户名">
            {{ selectedUser.username }}
          </a-descriptions-item>
          <a-descriptions-item label="邮箱">
            {{ selectedUser.email || '未设置' }}
          </a-descriptions-item>
          <a-descriptions-item label="角色">
            <a-tag :color="selectedUser.role === 'ADMIN' ? 'gold' : 'blue'">
              {{ selectedUser.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="selectedUser.status === 1 ? 'success' : 'error'">
              {{ selectedUser.status === 1 ? '正常' : '禁用' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">
            {{ formatDateTime(selectedUser.createdTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="最后更新">
            {{ formatDateTime(selectedUser.updatedTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="最后登录">
            {{ selectedUser.lastLoginTime ? formatDateTime(selectedUser.lastLoginTime) : '从未登录' }}
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ExportOutlined,
  ReloadOutlined
} from '@ant-design/icons-vue'
import { getUserList, createUser, updateUserInfo, updateUserStatus, deleteUser as deleteUserApi, resetUserPassword } from '@/api/admin'
import { checkUsernameExists } from '@/api/auth'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'

// 搜索和过滤
const searchKeyword = ref('')
const roleFilter = ref(undefined)
const statusFilter = ref(undefined)

// 表格数据
const userList = ref([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total, range) => `第 ${range[0]}-${range[1]} 条/共 ${total} 条`
})

// 表格列定义
const columns = [
  {
    title: '用户',
    dataIndex: 'username',
    key: 'username',
    width: 200
  },
  {
    title: '角色',
    dataIndex: 'role',
    key: 'role',
    width: 120
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100
  },
  {
    title: '最后登录',
    dataIndex: 'lastLoginTime',
    key: 'lastLoginTime',
    width: 180
  },
  {
    title: '创建时间',
    dataIndex: 'createdTime',
    key: 'createdTime',
    width: 180
  },
  {
    title: '操作',
    key: 'action',
    width: 300,
    fixed: 'right'
  }
]

// 创建用户表单
const showCreateModal = ref(false)
const creating = ref(false)
const createFormRef = ref()
const createForm = reactive({
  username: '',
  password: '',
  email: '',
  role: 'USER'
})

const createRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度必须在3-50个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入有效的邮箱格式', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

// 编辑用户表单
createRules.username.push({
  validator: async (_rule, value) => {
    if (!value || value.length < 3) {
      return Promise.resolve()
    }
    const response = await checkUsernameExists(value.trim())
    if (response.code === 200 && response.data?.exists) {
      return Promise.reject('用户名已存在')
    }
    return Promise.resolve()
  },
  trigger: 'blur'
})

const showEditModal = ref(false)
const editing = ref(false)
const editFormRef = ref()
const editForm = reactive({
  id: null,
  username: '',
  email: '',
  role: 'USER'
})

const editRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度必须在3-50个字符之间', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入有效的邮箱格式', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

// 详情模态框
const showDetailModal = ref(false)
const selectedUser = ref(null)

/**
 * 生成头像颜色
 */
const getAvatarColor = (username) => {
  const colors = ['#f56a00', '#7265e6', '#ffbf00', '#00a2ae', '#7cb305', '#1890ff']
  const index = username.charCodeAt(0) % colors.length
  return colors[index]
}

/**
 * 加载用户列表
 */
const loadUserList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.current,
      size: pagination.pageSize,
      keyword: searchKeyword.value,
      role: roleFilter.value,
      status: statusFilter.value
    }

    const response = await getUserList(params)
    if (response.code === 200) {
      userList.value = response.data.list
      pagination.total = response.data.total
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

/**
 * 处理搜索
 */
const handleSearch = () => {
  pagination.current = 1
  loadUserList()
}

/**
 * 处理筛选
 */
const handleFilter = () => {
  pagination.current = 1
  loadUserList()
}

/**
 * 处理表格变化
 */
const handleTableChange = (pag, filters, sorter) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadUserList()
}

/**
 * 处理刷新
 */
const handleRefresh = () => {
  searchKeyword.value = ''
  roleFilter.value = undefined
  statusFilter.value = undefined
  pagination.current = 1
  loadUserList()
}

/**
 * 创建用户
 */
const handleCreateUser = async () => {
  try {
    await createFormRef.value.validate()
    creating.value = true

    const params = {
      username: createForm.username,
      password: createForm.password,
      email: createForm.email,
      role: createForm.role
    }

    const response = await createUser(params)
    if (response.code === 200) {
      message.success('用户创建成功')
      showCreateModal.value = false
      createFormRef.value.resetFields()
      loadUserList()
    }
  } catch (error) {
    if (error.errorFields) {
      return
    }
    console.error('创建用户失败:', error)
    message.error('创建失败: ' + (error.message || '未知错误'))
  } finally {
    creating.value = false
  }
}

/**
 * 取消创建
 */
const handleCancelCreate = () => {
  showCreateModal.value = false
  createFormRef.value?.resetFields()
}

/**
 * 编辑用户
 */
const editUser = (record) => {
  editForm.id = record.id
  editForm.username = record.username
  editForm.email = record.email
  editForm.role = record.role
  showEditModal.value = true
}

/**
 * 保存编辑
 */
const handleEditUser = async () => {
  try {
    await editFormRef.value.validate()
    editing.value = true

    const params = {
      id: editForm.id,
      username: editForm.username,
      email: editForm.email,
      role: editForm.role
    }

    const response = await updateUserInfo(params.id, params)
    if (response.code === 200) {
      message.success('用户信息更新成功')
      showEditModal.value = false
      loadUserList()
    }
  } catch (error) {
    if (error.errorFields) {
      return
    }
    console.error('更新用户失败:', error)
    message.error('更新失败: ' + (error.message || '未知错误'))
  } finally {
    editing.value = false
  }
}

/**
 * 取消编辑
 */
const handleCancelEdit = () => {
  showEditModal.value = false
  editFormRef.value?.resetFields()
}

/**
 * 查看详情
 */
const viewDetails = (record) => {
  selectedUser.value = record
  showDetailModal.value = true
}

/**
 * 切换状态
 */
const toggleStatus = (record) => {
  const newStatus = record.status === 1 ? 0 : 1
  const actionText = newStatus === 1 ? '启用' : '禁用'

  Modal.confirm({
    title: `确认${actionText}`,
    content: `确定要${actionText}用户 ${record.username} 吗？`,
    onOk: async () => {
      try {
        const response = await updateUserStatus(record.id, newStatus)
        if (response.code === 200) {
          message.success(`${actionText}成功`)
          loadUserList()
        }
      } catch (error) {
        console.error(`${actionText}失败:`, error)
        message.error(`${actionText}失败`)
      }
    }
  })
}

/**
 * 重置密码
 */
const resetPassword = (record) => {
  Modal.confirm({
    title: '重置密码',
    content: `确定要重置用户 ${record.username} 的密码吗？`,
    onOk: async () => {
      try {
        const response = await resetUserPassword(record.id)
        if (response.code === 200) {
          message.success('密码重置成功，新密码已发送到用户邮箱')
        }
      } catch (error) {
        console.error('重置密码失败:', error)
        message.error('重置密码失败')
      }
    }
  })
}

/**
 * 删除用户
 */
const deleteUser = (record) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除用户 ${record.username} 吗？此操作不可恢复。`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      try {
        const response = await deleteUserApi(record.id)
        if (response.code === 200) {
          message.success('删除成功')
          loadUserList()
        }
      } catch (error) {
        console.error('删除失败:', error)
        message.error('删除失败')
      }
    }
  })
}

/**
 * 导出数据
 */
const handleExport = async () => {
  const exportMessageKey = 'user-export'
  try {
    message.loading({
      content: '正在导出数据...',
      key: exportMessageKey,
      duration: 0
    })

    // 构建导出参数
    const params = {
      keyword: searchKeyword.value,
      role: roleFilter.value,
      status: statusFilter.value,
      exportAll: true // 标记为导出全部数据
    }

    // 调用导出API
    const response = await getUserList(params)

    if (response.code === 200 && response.data.list) {
      exportUsersToCSV(response.data.list, '用户数据')
      message.success({
        content: '数据导出成功',
        key: exportMessageKey
      })
    } else {
      message.error({
        content: '导出失败：未获取到数据',
        key: exportMessageKey
      })
    }
  } catch (error) {
    console.error('导出数据失败:', error)
    message.error({
      content: '导出失败，请稍后重试',
      key: exportMessageKey
    })
  }
}

/**
 * 导出用户数据为CSV
 */
const exportUsersToCSV = (data, filename) => {
  if (!data || data.length === 0) {
    message.warning('没有数据可导出')
    return
  }

  // CSV表头
  const headers = [
    '用户ID',
    '用户名',
    '邮箱',
    '角色',
    '状态',
    '最后登录时间',
    '创建时间',
    '更新时间'
  ]

  // 数据转换
  const csvData = data.map(item => [
    item.id || '',
    item.username || '',
    item.email || '',
    item.role === 'ADMIN' ? '管理员' : '普通用户',
    item.status === 1 ? '正常' : '禁用',
    item.lastLoginTime ? formatDateTime(item.lastLoginTime) : '从未登录',
    formatDateTime(item.createdTime),
    item.updatedTime ? formatDateTime(item.updatedTime) : ''
  ])

  // 构建CSV内容
  const csvContent = [
    headers.join(','),
    ...csvData.map(row => row.map(field => `"${String(field).replace(/"/g, '""')}"`).join(','))
  ].join('\n')

  // 添加BOM以支持中文
  const BOM = '\uFEFF'
  const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })

  // 创建下载链接
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.setAttribute('href', url)
  link.setAttribute('download', `${filename}_${new Date().toISOString().slice(0, 10)}.csv`)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  // 清理URL对象
  URL.revokeObjectURL(url)
}

/**
 * 格式化日期时间
 */
const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  return new Date(dateTime).toLocaleString()
}

/**
 * 格式化相对时间
 */
const formatTimeAgo = (dateTime) => {
  if (!dateTime) return ''
  const now = new Date()
  const diff = now - new Date(dateTime)
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (days > 0) return `${days}天前`
  if (hours > 0) return `${hours}小时前`
  if (minutes > 0) return `${minutes}分钟前`
  return '刚刚'
}

/**
 * 生命周期钩子
 */
onMounted(() => {
  loadUserList()
})
</script>

<style scoped>
.user-manage {
  min-height: 100%;
  padding: 4px;
}

.filter-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.table-card {
  border-radius: 8px;
}

.user-cell {
  display: flex;
  align-items: center;
}

.user-info {
  margin-left: 12px;
}

.username {
  font-weight: 500;
  color: #262626;
}

.email {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}

.time-cell {
  line-height: 1.4;
}

.time-ago {
  font-size: 12px;
  color: #8c8c8c;
}

.action-cell {
  white-space: nowrap;
}

.detail-content {
  padding: 16px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-manage {
    padding: 0;
  }

  .action-buttons {
    margin-top: 16px;
    justify-content: flex-start;
  }
}
</style>
