import { defineConfig } from 'vite';

/** 前端开发服务器将接口请求代理到本地 Spring Boot，避免开发阶段跨域。 */
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/actuator': 'http://localhost:8080'
    }
  }
});
