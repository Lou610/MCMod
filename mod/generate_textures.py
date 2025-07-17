#!/usr/bin/env python3
"""
Simple texture generator for MC Mod
Creates basic 16x16 pixel textures for items and blocks
"""

from PIL import Image, ImageDraw
import os

def create_item_texture(filename, color, pattern="solid"):
    """Create a 16x16 item texture"""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    if pattern == "solid":
        draw.rectangle([2, 2, 13, 13], fill=color, outline=(0, 0, 0, 255))
    elif pattern == "shiny":
        # Create a shiny effect
        draw.rectangle([2, 2, 13, 13], fill=color, outline=(0, 0, 0, 255))
        draw.rectangle([4, 4, 11, 6], fill=(255, 255, 255, 100))
    
    # Ensure directory exists
    os.makedirs(os.path.dirname(filename), exist_ok=True)
    img.save(filename)
    print(f"Created texture: {filename}")

def create_block_texture(filename, color, pattern="solid"):
    """Create a 16x16 block texture"""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    if pattern == "solid":
        draw.rectangle([0, 0, 15, 15], fill=color, outline=(0, 0, 0, 255))
    elif pattern == "stone":
        # Create a stone-like pattern
        draw.rectangle([0, 0, 15, 15], fill=color, outline=(0, 0, 0, 255))
        # Add some noise
        for i in range(5):
            x = (i * 3) % 16
            y = (i * 2) % 16
            draw.point((x, y), fill=(0, 0, 0, 100))
    
    # Ensure directory exists
    os.makedirs(os.path.dirname(filename), exist_ok=True)
    img.save(filename)
    print(f"Created texture: {filename}")

def main():
    """Generate all textures for the mod"""
    base_path = "src/main/resources/assets/mcmod/textures"
    
    # Create item texture (blue color)
    create_item_texture(
        f"{base_path}/item/custom_item.png",
        (100, 150, 255, 255),
        "shiny"
    )
    
    # Create block texture (dark blue color)
    create_block_texture(
        f"{base_path}/block/custom_block.png",
        (50, 100, 200, 255),
        "stone"
    )
    
    print("All textures generated successfully!")

if __name__ == "__main__":
    main() 