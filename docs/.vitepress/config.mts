import { defineConfig } from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "Web后台性能探索",
  description: "从单体服务到微服务",
  base: '/hp/',
  themeConfig: {
    // https://vitepress.dev/reference/default-theme-config
    nav: [
      { text: '单体服务', link: 'one/intro' },
      { text: '微服务', link: 'cloud/intro' },
    ],

    sidebar: [
      {text: '单体服务', items: [
        {text: '性能基准测试', link: 'one/00_base_benchmark'},
        {text: '电商业务梳理(程序员可跳过)', link: 'one/01_business_abstruct'}, 
        {text: '下单业务与代码实现', link: 'one/02_place_order'},
        {text: '下单接口性能提升', link: 'one/03_place_order_enhance'},
        {text: '总结', link: 'one/04_advanced'},      
      ], link: 'one/intro '},
      {text: '微服务', items: [
        {text: '单服务到微服务', link: 'cloud/00_change_from_one'},
        {text: '微服务拆分', link: 'cloud/01_split'},
        {text: '性能测试', link: 'cloud/02_benchmark'},
        {text: '总结', link: 'cloud/03_advanced'},
      ], link: 'cloud/intro'}
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/timzaak/hp' }
    ]
  }
})
