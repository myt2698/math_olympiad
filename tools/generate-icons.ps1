param([string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot))

Add-Type -AssemblyName System.Drawing

function New-AppIcon([int]$Size, [string]$OutputPath) {
  $bitmap = New-Object System.Drawing.Bitmap $Size, $Size
  $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
  $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $purple = [System.Drawing.ColorTranslator]::FromHtml('#5b5bd6')
  $yellow = [System.Drawing.ColorTranslator]::FromHtml('#ffca57')
  $white = [System.Drawing.Color]::White
  $graphics.Clear($purple)
  $planetBrush = New-Object System.Drawing.SolidBrush $yellow
  $graphics.FillEllipse($planetBrush, $Size * .22, $Size * .22, $Size * .56, $Size * .56)
  $ringPen = New-Object System.Drawing.Pen $white, ($Size * .045)
  $graphics.DrawEllipse($ringPen, $Size * .10, $Size * .36, $Size * .80, $Size * .28)
  $starBrush = New-Object System.Drawing.SolidBrush $white
  $points = @(
    [System.Drawing.PointF]::new($Size*.50,$Size*.34), [System.Drawing.PointF]::new($Size*.54,$Size*.46),
    [System.Drawing.PointF]::new($Size*.66,$Size*.50), [System.Drawing.PointF]::new($Size*.54,$Size*.54),
    [System.Drawing.PointF]::new($Size*.50,$Size*.66), [System.Drawing.PointF]::new($Size*.46,$Size*.54),
    [System.Drawing.PointF]::new($Size*.34,$Size*.50), [System.Drawing.PointF]::new($Size*.46,$Size*.46)
  )
  $graphics.FillPolygon($starBrush, $points)
  $bitmap.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
  $starBrush.Dispose(); $ringPen.Dispose(); $planetBrush.Dispose(); $graphics.Dispose(); $bitmap.Dispose()
}

$iconDir = Join-Path $ProjectRoot 'icons'
New-Item -ItemType Directory -Force -Path $iconDir | Out-Null
New-AppIcon 192 (Join-Path $iconDir 'icon-192.png')
New-AppIcon 512 (Join-Path $iconDir 'icon-512.png')
