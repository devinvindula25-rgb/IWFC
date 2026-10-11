from PIL import Image, ImageDraw, ImageFont
import sys

img_path = r"C:\Users\devin\.gemini\antigravity\brain\401128c2-6698-417a-ae3a-498a12f247f2\iwfc_eagle_logo_1791432175981.jpg"
out_path = r"C:\Users\devin\.gemini\antigravity\brain\401128c2-6698-417a-ae3a-498a12f247f2\iwfc_eagle_logo_with_text.jpg"

try:
    img = Image.open(img_path)
    width, height = img.size
    
    # Sample the background color from the top left corner
    bg_color = img.getpixel((10, 10))
    
    # Create new canvas (taller) to fit text
    new_height = height + 280
    new_img = Image.new("RGB", (width, new_height), bg_color)
    new_img.paste(img, (0, 0))
    
    draw = ImageDraw.Draw(new_img)
    
    # Load fonts
    try:
        font = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 180) # Segoe UI Bold
        sub_font = ImageFont.truetype("C:/Windows/Fonts/segoeuil.ttf", 45) # Segoe UI Light
    except:
        font = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 180)
        sub_font = ImageFont.truetype("C:/Windows/Fonts/arial.ttf", 45)

    text = "IWFC"
    sub_text = "INTELLIGENT WELLNESS & FITNESS CENTER"
    
    # Get bounding boxes for centering
    text_bbox = draw.textbbox((0, 0), text, font=font)
    text_w = text_bbox[2] - text_bbox[0]
    
    sub_bbox = draw.textbbox((0, 0), sub_text, font=sub_font)
    sub_w = sub_bbox[2] - sub_bbox[0]
    
    # Draw main text (Cyan to match eagle)
    text_x = (width - text_w) / 2
    text_y = height - 20
    draw.text((text_x, text_y), text, font=font, fill=(43, 200, 255))
    
    # Draw subtext (White/Silver)
    sub_x = (width - sub_w) / 2
    sub_y = text_y + 210
    draw.text((sub_x, sub_y), sub_text, font=sub_font, fill=(220, 226, 230))
    
    new_img.save(out_path)
    print(f"Saved to {out_path}")
except Exception as e:
    print(f"Error: {e}")
