from PIL import Image, ImageDraw, ImageFont
import os

# Create icons directory if not exists
os.makedirs('icons', exist_ok=True)

# Create 32x32 icons with language codes
languages = {
    'en': ('English', '#FF0000'),  # Red
    'de': ('Deutsch', '#000000'),  # Black
    'zh': ('中文', '#0000FF')       # Blue
}

size = (32, 32)

for lang_code, (lang_name, color) in languages.items():
    # Create new image with transparent background
    img = Image.new('RGBA', size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Draw a colored circle as background
    margin = 2
    draw.ellipse([margin, margin, size[0]-margin, size[1]-margin], 
                 fill=color, outline='white', width=2)
    
    # Add white text (language code)
    try:
        # Try to use a font, fallback to default if not available
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 14)
    except:
        font = ImageFont.load_default()
    
    text = lang_code.upper()
    
    # Calculate text position
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]
    text_x = (size[0] - text_width) // 2
    text_y = (size[1] - text_height) // 2
    
    # Draw text with shadow
    draw.text((text_x+1, text_y+1), text, font=font, fill='black')
    draw.text((text_x, text_y), text, font=font, fill='white')
    
    # Save the image
    img.save(f'icons/lang_{lang_code}.png')
    print(f'Created icons/lang_{lang_code}.png')

print('All language icons created successfully!')
