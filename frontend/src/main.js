import { createApp } from 'vue'
import {
  Alert,
  Avatar,
  BackTop,
  Breadcrumb,
  Button,
  Card,
  Col,
  ConfigProvider,
  DatePicker,
  Descriptions,
  Divider,
  Dropdown,
  Empty,
  Form,
  Input,
  Layout,
  List,
  Menu,
  Modal,
  Progress,
  Radio,
  Row,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Tooltip
} from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import './assets/styles/global.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

const components = [
  Alert,
  Avatar,
  BackTop,
  Breadcrumb,
  Breadcrumb.Item,
  Button,
  Card,
  Col,
  ConfigProvider,
  DatePicker,
  DatePicker.RangePicker,
  Descriptions,
  Descriptions.Item,
  Divider,
  Dropdown,
  Empty,
  Form,
  Form.Item,
  Input,
  Input.Password,
  Input.Search,
  Layout,
  Layout.Content,
  Layout.Footer,
  Layout.Header,
  Layout.Sider,
  List,
  List.Item,
  List.Item.Meta,
  Menu,
  Menu.Divider,
  Menu.Item,
  Modal,
  Progress,
  Radio,
  Radio.Button,
  Radio.Group,
  Row,
  Select,
  Select.Option,
  Space,
  Spin,
  Table,
  Tag,
  Tooltip
]

components.forEach((component) => {
  if (component?.name) {
    app.component(component.name, component)
  }
})

app.config.errorHandler = (err, instance, info) => {
  console.error('Vue application error:', err)
  console.error('Error info:', info)
}

app.config.globalProperties = {
  appVersion: '1.0.0',
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || '/api'
}

app.mount('#app')

if (import.meta.env.PROD) {
  window.addEventListener('load', () => {
    if ('performance' in window) {
      const loadTime = performance.now()
      console.log(`Application load time: ${loadTime}ms`)
    }
  })
}
