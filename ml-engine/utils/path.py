import os
import sys

def resource_path(relative_path: str) -> str:
    """
    Works in dev mode and PyInstaller executable
    """
    if hasattr(sys, "_MEIPASS"):
        return os.path.join(sys._MEIPASS, relative_path)
    return os.path.join(os.getcwd(), relative_path)
