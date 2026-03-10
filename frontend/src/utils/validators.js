/**
 * 表单验证工具函数
 *
 * 模块职责：
 * - 提供常用的表单验证规则
 * - 统一的错误提示信息
 * - 支持自定义验证逻辑
 */

/**
 * URL验证规则
 */
export const urlRules = [
  {
    required: true,
    message: '请输入URL',
    trigger: 'blur'
  },
  {
    type: 'url',
    message: '请输入有效的URL格式，如：https://example.com',
    trigger: 'blur'
  },
  {
    validator: (rule, value) => {
      if (value && value.length > 2048) {
        return Promise.reject('URL长度不能超过2048个字符')
      }
      return Promise.resolve()
    },
    trigger: 'blur'
  }
]

/**
 * 用户名验证规则
 */
export const usernameRules = [
  {
    required: true,
    message: '请输入用户名',
    trigger: 'blur'
  },
  {
    min: 3,
    max: 50,
    message: '用户名长度必须在3-50个字符之间',
    trigger: 'blur'
  },
  {
    pattern: /^[a-zA-Z0-9_\u4e00-\u9fa5]+$/,
    message: '用户名只能包含字母、数字、下划线和中文',
    trigger: 'blur'
  }
]

/**
 * 密码验证规则
 */
export const passwordRules = [
  {
    required: true,
    message: '请输入密码',
    trigger: 'blur'
  },
  {
    min: 6,
    max: 50,
    message: '密码长度必须在6-50个字符之间',
    trigger: 'blur'
  },
  {
    pattern: /^(?=.*[a-zA-Z])(?=.*\d)/,
    message: '密码必须包含字母和数字',
    trigger: 'blur'
  }
]

/**
 * 邮箱验证规则
 */
export const emailRules = [
  {
    type: 'email',
    message: '请输入有效的邮箱格式',
    trigger: 'blur'
  }
]

/**
 * 标题验证规则
 */
export const titleRules = [
  {
    max: 255,
    message: '标题长度不能超过255个字符',
    trigger: 'blur'
  }
]

/**
 * 角色验证规则
 */
export const roleRules = [
  {
    required: true,
    message: '请选择角色',
    trigger: 'change'
  }
]

/**
 * 状态验证规则
 */
export const statusRules = [
  {
    required: true,
    message: '请选择状态',
    trigger: 'change'
  }
]

/**
 * 必填字段验证规则
 */
export const requiredRules = (fieldName) => [
  {
    required: true,
    message: `请输入${fieldName}`,
    trigger: 'blur'
  }
]

/**
 * 选择字段验证规则
 */
export const selectRequiredRules = (fieldName) => [
  {
    required: true,
    message: `请选择${fieldName}`,
    trigger: 'change'
  }
]

/**
 * 自定义URL验证函数
 */
export const validateUrl = (url) => {
  try {
    new URL(url)
    return true
  } catch {
    return false
  }
}

/**
 * 自定义邮箱验证函数
 */
export const validateEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email)
}

/**
 * 自定义密码强度验证
 */
export const validatePasswordStrength = (password) => {
  const hasLetter = /[a-zA-Z]/.test(password)
  const hasNumber = /\d/.test(password)
  const hasSpecialChar = /[!@#$%^&*(),.?":{}|<>]/.test(password)
  const isLongEnough = password.length >= 8

  const score = [hasLetter, hasNumber, hasSpecialChar, isLongEnough].filter(Boolean).length

  if (score >= 4) return 'strong'
  if (score >= 3) return 'medium'
  return 'weak'
}

/**
 * 获取密码强度提示
 */
export const getPasswordStrengthTip = (strength) => {
  const tips = {
    strong: { color: 'success', text: '强' },
    medium: { color: 'warning', text: '中' },
    weak: { color: 'error', text: '弱' }
  }
  return tips[strength] || tips.weak
}

/**
 * 表单验证错误处理
 */
export const handleFormError = (errorInfo) => {
  console.error('表单验证失败:', errorInfo)

  // 提取第一个错误字段的错误信息
  const firstError = errorInfo.errorFields?.[0]?.errors?.[0]
  if (firstError) {
    return firstError
  }

  return '请检查表单输入'
}

/**
 * 防抖验证函数
 */
export const debounceValidation = (fn, delay = 300) => {
  let timeoutId
  return (...args) => {
    clearTimeout(timeoutId)
    timeoutId = setTimeout(() => fn.apply(this, args), delay)
  }
}

/**
 * 实时URL验证（防抖）
 */
export const debouncedUrlValidation = debounceValidation((url) => {
  if (!url) return Promise.resolve()

  return new Promise((resolve, reject) => {
    // 简单的URL可达性检查（仅格式验证）
    if (validateUrl(url)) {
      resolve()
    } else {
      reject('请输入有效的URL格式')
    }
  })
})

/**
 * 实时用户名重复检查（防抖）
 */
export const debouncedUsernameCheck = debounceValidation(async (username) => {
  if (!username) return Promise.resolve()

  try {
    // TODO: 调用API检查用户名是否已存在
    // const response = await checkUsernameExists(username)
    // if (response.data.exists) {
    //   return Promise.reject('用户名已存在')
    // }
    return Promise.resolve()
  } catch (error) {
    return Promise.reject('检查用户名失败，请稍后重试')
  }
}, 500)

/**
 * 统一的成功提示
 */
export const showSuccessMessage = (message) => {
  // 这里可以接入全局的message实例
  console.log('Success:', message)
}

/**
 * 统一的错误提示
 */
export const showErrorMessage = (message) => {
  // 这里可以接入全局的message实例
  console.error('Error:', message)
}

/**
 * 统一的警告提示
 */
export const showWarningMessage = (message) => {
  // 这里可以接入全局的message实例
  console.warn('Warning:', message)
}