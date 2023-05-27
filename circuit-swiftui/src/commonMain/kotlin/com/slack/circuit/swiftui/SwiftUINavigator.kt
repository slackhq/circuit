package com.slack.circuit.swiftui

import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.Screen
import com.slack.circuit.swiftui.objc.CircuitSwiftUINavigatorProtocol
import platform.darwin.NSObject

public class SwiftUINavigator internal constructor(): Navigator {

    internal var navigator: CircuitSwiftUINavigatorProtocol? = null

    private fun requireNavigator(): CircuitSwiftUINavigatorProtocol =
        navigator ?: throw RuntimeException("SwiftUINavigator hasn't been initialized")

    override fun goTo(screen: Screen): Unit = requireNavigator().goToScreen(screen as NSObject)

    override fun pop(): Screen? = requireNavigator().pop() as Screen?

    override fun resetRoot(newRoot: Screen): List<Screen> =
        requireNavigator().resetRootNewRoot(newRoot as NSObject).map { it as Screen }
}
