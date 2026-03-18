import { checkUsernameExists } from '@/api/auth'

/**
 * 表单校验工具
 */

export const urlRules = [
  { required: true, message: '请输入URL', trigger: 'blur' },
  { type: 'url', message: '请输入有效的URL格式，例如：https://example.com', trigger: 'blur' },
  {
    validator: (_rule, value) => {
      if (value && value.length > 2048) {
        return Promise.reject('URL长度不能超过2048个字符')
      }
      return Promise.resolve()
    },
    trigger: 'blur'
  }
]

export const usernameRules = [
  { required: true, message: '请输入用户名', trigger: 'blur' },
  { min: 3, max: 50, message: '用户名长度必须在3-50个字符之间', trigger: 'blur' },
  { pattern: /^[a-zA-Z0-9_\u4e00-\u9fa5]+$/, message: '用户名只能包含字母、数字、下划线和中文', trigger: 'blur' }
]

export const passwordRules = [
  { required: true, message: '请输入密码', trigger: 'blur' },
  { min: 6, max: 50, message: '密码长度必须在6-50个字符之间', trigger: 'blur' },
  { pattern: /^(?=.*[a-zA-Z])(?=.*\d)/, message: '密码必须包含字母和数字', trigger: 'blur' }
]

export const emailRules = [{ type: 'email', message: '请输入有效的邮箱格式', trigger: 'blur' }]

export const titleRules = [{ max: 255, message: '标题长度不能超过255个字符', trigger: 'blur' }]

export const roleRules = [{ required: true, message: '请选择角色', trigger: 'change' }]

export const statusRules = [{ required: true, message: '请选择状态', trigger: 'change' }]

export const requiredRules = (fieldName) => [{ required: true, message: `请输入${fieldName}`, trigger: 'blur' }]

export const selectRequiredRules = (fieldName) => [{ required: true, message: `请选择${fieldName}`, trigger: 'change' }]

export const validateUrl = (url) => {
  try {
    new URL(url)
    return true
  } catch {
    return false
  }
}

export const validateEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email)
}

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

export const getPasswordStrengthTip = (strength) => {
  const tips = {
    strong: { color: 'success', text: '强' },
    medium: { color: 'warning', text: '中' },
    weak: { color: 'error', text: '弱' }
  }
  return tips[strength] || tips.weak
}

export const handleFormError = (errorInfo) => {
  console.error('表单校验失败:', errorInfo)
  const firstError = errorInfo.errorFields?.[0]?.errors?.[0]
  return firstError || '请检查表单输入'
}

export const debounceValidation = (fn, delay = 300) => {
  let timeoutId
  return (...args) => {
    clearTimeout(timeoutId)
    timeoutId = setTimeout(() => fn.apply(this, args), delay)
  }
}

export const debouncedUrlValidation = debounceValidation((url) => {
  if (!url) return Promise.resolve()
  return validateUrl(url) ? Promise.resolve() : Promise.reject('请输入有效的URL格式')
})

export const debouncedUsernameCheck = debounceValidation(async (username) => {
  if (!username) return Promise.resolve()

  try {
    const response = await checkUsernameExists(username)
    if (response.code === 200 && response.data?.exists) {
      return Promise.reject('用户名已存在')
    }
    return Promise.resolve()
  } catch (error) {
    return Promise.reject(error?.message || '检查用户名失败，请稍后重试')
  }
}, 500)

export const showSuccessMessage = (message) => {
  console.log('Success:', message)
}

export const showErrorMessage = (message) => {
  console.error('Error:', message)
}

export const showWarningMessage = (message) => {
  console.warn('Warning:', message)
}
