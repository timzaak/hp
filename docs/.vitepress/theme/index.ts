import DefaultTheme from 'vitepress/theme'
import {EnhanceAppContext} from "vitepress";

export default {
    ...DefaultTheme,
    enhanceApp(ctx:EnhanceAppContext) {
        DefaultTheme.enhanceApp(ctx);
        const gh = "https://github.com/timzaak/hp"
        ctx.app.config.globalProperties.gh = gh
        ctx.app.config.globalProperties.luaSrc = gh + "/blob/main/backend/src/main/lua"
        ctx.app.config.globalProperties.codeSrc = gh + "/blob/main/backend/src/main/java/com/timzaak/backend"
        ctx.app.config.globalProperties.resourceSrc = gh + "/blob/main/backend/src/main/resources"
        ctx.app.config.globalProperties.benchSrc = gh + "/blob/main/benchmark/src/test/scala/com/timzaak"
    }
}