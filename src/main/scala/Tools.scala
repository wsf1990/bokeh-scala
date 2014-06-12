package org.continuumio.bokeh

sealed abstract class Tool extends PlotObject {
    object plot extends Field[Plot]
}

class PanTool extends Tool {
    object dimensions extends Field[List[Dimension]](List(Dimension.Width, Dimension.Height))
}

class WheelZoomTool extends Tool {
    object dimensions extends Field[List[Dimension]](List(Dimension.Width, Dimension.Height))
}

class PreviewSaveTool extends Tool

class EmbedTool extends Tool

class ResetTool extends Tool

class ResizeTool extends Tool

class CrosshairTool extends Tool

class BoxZoomTool extends Tool {
    object renderers extends Field[List[Renderer]]
    object select_every_mousemove extends Field[Boolean](true)
}

class BoxSelectTool extends Tool {
    object renderers extends Field[List[Renderer]]
    object select_every_mousemove extends Field[Boolean](true)
}

class HoverTool extends Tool {
    object renderers extends Field[List[Renderer]]
    object tooltips extends Field[Map[String, String]]
}

class ObjectExplorerTool extends Tool

class DataRangeBoxSelectTool extends Tool {
    object xselect extends Field[List[Range]]
    object yselect extends Field[List[Range]]
}
