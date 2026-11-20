#!/usr/bin/env python3
"""Render the official Android vector icon and pad portrait store screenshots.

Only store metadata is modified; no user-facing app resources are changed.
"""
from __future__ import annotations

import math
from pathlib import Path
from xml.etree import ElementTree as ET

import cairosvg
from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[1]
METADATA = ROOT / "fastlane" / "metadata" / "android"
SOURCE_ICON = ROOT / "app" / "src" / "main" / "res" / "mipmap" / "ic_launcher.xml"
SCREENSHOTS = METADATA / "es-ES" / "images" / "phoneScreenshots"
ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
SVG_NS = "http://www.w3.org/2000/svg"
BACKGROUND_COLOR = "#160C2A"


def android_attr(element: ET.Element, key: str, default: str = "") -> str:
    return element.attrib.get(ANDROID_NS + key, default)


def convert_element(node: ET.Element, parent: ET.Element) -> None:
    local_name = node.tag.split("}")[-1]
    if local_name == "path":
        path_data = android_attr(node, "pathData")
        if not path_data:
            raise ValueError("Vector path is missing pathData")
        attributes = {
            "d": path_data,
            "fill": android_attr(node, "fillColor", "none"),
        }
        if alpha := android_attr(node, "fillAlpha"):
            attributes["fill-opacity"] = alpha
        ET.SubElement(parent, f"{{{SVG_NS}}}path", attributes)
    elif local_name == "group":
        scale_x = android_attr(node, "scaleX", "1")
        scale_y = android_attr(node, "scaleY", "1")
        translate_x = android_attr(node, "translateX", "0")
        translate_y = android_attr(node, "translateY", "0")
        rotation = android_attr(node, "rotation", "0")
        pivot_x = android_attr(node, "pivotX", "0")
        pivot_y = android_attr(node, "pivotY", "0")
        # Android VectorDrawable groups apply scale around pivot before translation.
        transform = (
            f"translate({translate_x} {translate_y}) "
            f"translate({pivot_x} {pivot_y}) "
            f"rotate({rotation}) scale({scale_x} {scale_y}) "
            f"translate(-{pivot_x} -{pivot_y})"
        )
        group = ET.SubElement(parent, f"{{{SVG_NS}}}g", {"transform": transform})
        for child in node:
            convert_element(child, group)
    else:
        raise ValueError(f"Unsupported Android VectorDrawable node: {local_name}")


def render_icon() -> None:
    android_vector = ET.parse(SOURCE_ICON).getroot()
    width = android_attr(android_vector, "viewportWidth")
    height = android_attr(android_vector, "viewportHeight")
    if not width or not height:
        raise ValueError("Vector icon viewport is missing")
    svg_root = ET.Element(
        f"{{{SVG_NS}}}svg",
        {
            "xmlns": SVG_NS,
            "width": width,
            "height": height,
            "viewBox": f"0 0 {width} {height}",
        },
    )
    for child in android_vector:
        convert_element(child, svg_root)
    svg_bytes = ET.tostring(svg_root, encoding="utf-8", xml_declaration=True)
    icon = cairosvg.svg2png(
        bytestring=svg_bytes,
        output_width=512,
        output_height=512,
    )
    for locale in ("en-US", "es-ES"):
        destination = METADATA / locale / "images" / "icon.png"
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(icon)
        with Image.open(destination) as verify:
            if verify.size != (512, 512) or verify.format != "PNG":
                raise ValueError(f"Unexpected icon format: {destination}")
        print(f"Icon: {destination.relative_to(ROOT)}")


def normalize_screenshots() -> None:
    expected = [SCREENSHOTS / f"{number}.jpg" for number in range(1, 6)]
    for screenshot in expected:
        if not screenshot.is_file():
            raise FileNotFoundError(f"Missing required screenshot: {screenshot}")
        with Image.open(screenshot) as source:
            source.load()
            width, height = source.size
            if width <= 0 or height <= 0:
                raise ValueError(f"Invalid screenshot dimensions: {screenshot}")
            if height / width <= 2.0:
                print(f"Valid: {screenshot.name}, {width}x{height}")
                continue
            if height < width:
                raise ValueError(f"Cannot pad landscape screenshot: {screenshot}")
            target_width = max(width, math.ceil(height / 2.0))
            padded = Image.new("RGB", (target_width, height), BACKGROUND_COLOR)
            padded.paste(source.convert("RGB"), ((target_width - width) // 2, 0))
            padded.save(screenshot, format="JPEG", quality=95, subsampling=0, optimize=True)
        with Image.open(screenshot) as verified:
            if verified.height / verified.width > 2.0:
                raise AssertionError(f"Screenshot still too tall: {screenshot}")
            print(
                f"Padded: {screenshot.name}: {width}x{height}"
                f" -> {verified.width}x{verified.height}"
            )


if __name__ == "__main__":
    render_icon()
    normalize_screenshots()
