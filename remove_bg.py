from PIL import Image, ImageDraw
import sys

input_path = r"C:\Users\devin\.gemini\antigravity\brain\401128c2-6698-417a-ae3a-498a12f247f2\iwfc_eagle_logo_with_text.jpg"
output_path = r"C:\Users\devin\.gemini\antigravity\brain\401128c2-6698-417a-ae3a-498a12f247f2\iwfc_eagle_logo_transparent.png"

try:
    img = Image.open(input_path).convert("RGBA")
    width, height = img.size
    
    # We use flood fill from the edges to remove the dark background
    ImageDraw.floodfill(img, (0, 0), (255, 0, 255, 0), thresh=30)
    ImageDraw.floodfill(img, (width-1, 0), (255, 0, 255, 0), thresh=30)
    
    pixels = img.load()
    for y in range(height):
        for x in range(width):
            if pixels[x, y] == (255, 0, 255, 0):
                pixels[x, y] = (0, 0, 0, 0)
            else:
                r, g, b, a = pixels[x, y]
                # Soften the edges by removing near-black pixels left over
                if r < 30 and g < 30 and b < 40:
                    alpha = int((b / 40.0) * 255)
                    pixels[x, y] = (r, g, b, alpha)
                
    img.save(output_path, "PNG")
    print(f"Saved transparent image to {output_path}")
except Exception as e:
    print(f"Error: {e}")
