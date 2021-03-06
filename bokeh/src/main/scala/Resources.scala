package io.continuum.bokeh

import java.net.URI

import scalatags.Text.short.Tag

sealed abstract class ResourceComponent(val name: String) {
    val js = true
    val css = true
}
case object BokehCore extends ResourceComponent("bokeh")
case object BokehWidgets extends ResourceComponent("bokeh-widgets")
case object BokehCompiler extends ResourceComponent("bokeh-compiler") {
    override val css = false
}

case class Bundle(js: Seq[JSArtifact], css: Seq[CSSArtifact]) {
    def scripts: Seq[Tag] = js.map {
        case RefJS(src) => src.asScript
        case RawJS(text) => text.asScript
    }

    def styles: Seq[Tag] = css.map {
        case RefCSS(src) => src.asStyle
        case RawCSS(text) => text.asStyle
    }

    def jsUrls = js.collect { case RefJS(src) => src }
    def jsRaw = js.collect { case RawJS(text) => text }

    def cssUrls = css.collect { case RefCSS(src) => src }
    def cssRaw = css.collect { case RawCSS(text) => text }
}

sealed trait Artifact
sealed trait JSArtifact extends Artifact
sealed trait CSSArtifact extends Artifact
case class RefJS(src: URI) extends JSArtifact
case class RefCSS(src: URI) extends CSSArtifact
case class RawJS(text: String) extends JSArtifact
case class RawCSS(text: String) extends CSSArtifact

trait Resources {
    def minified: Boolean = true

    def logLevel: LogLevel = LogLevel.Info

    val indent = 0

    def stringify[T:Json.Writer](obj: T): String = {
        Json.write(obj, indent=indent)
    }

    def wrap(code: String): String = s"Bokeh.$$(function() {\n$code\n});"

    protected def logLevelScript: JSArtifact = {
        RawJS(s"Bokeh.set_log_level('$logLevel');")
    }

    def bundle(refs: List[Model]): Bundle = {
        val components = (Some(BokehCore) :: useWidgets(refs) :: useCompiler(refs) :: Nil).flatten

        val scripts = components.filter(_.js == true).map(resolveScript) ++ List(logLevelScript)
        val styles = components.filter(_.css == true).map(resolveStyle)

        Bundle(scripts, styles)
    }

    def useWidgets(refs: List[Model]): Option[ResourceComponent] = {
        refs.collectFirst { case ref: widgets.Widget => ref }
            .map { _ => BokehWidgets }
    }

    def useCompiler(refs: List[Model]): Option[ResourceComponent] = {
        refs.collect { case ref: CustomModel => ref.implementation }
            .collectFirst { case impl@CoffeeScript(_) => impl }
            .map { _ => BokehCompiler }
    }

    protected def resolveScript(component: ResourceComponent): JSArtifact
    protected def resolveStyle(component: ResourceComponent): CSSArtifact

    protected def resourceName(component: ResourceComponent, ext: String, version: Boolean=false) = {
        val ver = if (version) s"-$Version" else ""
        val min = if (minified) ".min" else ""
        s"${component.name}$ver$min.$ext"
    }
}

trait DevResources { self: Resources =>
    override val minified = false

    override val logLevel = LogLevel.Debug

    override val indent = 2
}

abstract class RemoteResources(url: URI) extends Resources {
    def resolveScript(component: ResourceComponent) = {
        RefJS(new URI(url + "/" + resourceName(component, "js", true)))
    }

    def resolveStyle(component: ResourceComponent) = {
        RefCSS(new URI(url + "/" + resourceName(component, "css", true)))
    }
}

abstract class CDNResources extends RemoteResources(new URI("http://cdn.pydata.org/bokeh/release"))

trait KnownResources {
    def default: Resources

    protected val fromStringPF: PartialFunction[String, Resources]

    def fromString(string: String): Option[Resources] = fromStringPF.lift(string)
}
