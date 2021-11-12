package de.solidblocks.cli.config

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component

@Component
class SpringContextUtil : ApplicationContextAware {

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        context = applicationContext
    }

    companion object {
        lateinit var context: ApplicationContext

        fun <T> callBeanAndShutdown(type: Class<T>, block: (T) -> Unit) {
            block.invoke(bean(type))
            (context as ConfigurableApplicationContext).close()
        }

        fun <T> bean(type: Class<T>): T =
            context.getBean(type)

        fun <T> bean(type: Class<T>, name: String): T =
            context.getBean(name, type)
    }
}
