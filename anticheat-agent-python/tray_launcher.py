import os
import signal
import sys
import threading
import time

import pystray
from PIL import Image, ImageDraw

from input_capture import CaptureAgent


def _create_icon_image():
    image = Image.new("RGB", (64, 64), (15, 23, 42))
    draw = ImageDraw.Draw(image)
    draw.rectangle((8, 8, 56, 56), fill=(30, 41, 59), outline=(239, 68, 68), width=3)
    draw.text((22, 18), "A", fill=(239, 68, 68))
    return image


class TrayApp:
    def __init__(self):
        self.agent = CaptureAgent(os.getenv("AC_SERVER_URL"))
        self._agent_thread = None
        self.icon = pystray.Icon(
            "AEGISAntiCheatAgent",
            _create_icon_image(),
            "AEGIS AntiCheat Agent",
            menu=pystray.Menu(
                pystray.MenuItem("Status: Running", self._noop, enabled=False),
                pystray.MenuItem("Open Dashboard", self._open_dashboard),
                pystray.MenuItem("Restart Agent", self._restart_agent),
                pystray.MenuItem("Exit", self._exit),
            ),
        )

    def start(self):
        self._start_agent()
        self.icon.run()

    def _start_agent(self):
        self.agent.start()
        self._agent_thread = threading.Thread(target=self.agent.wait, daemon=True)
        self._agent_thread.start()

    def _restart_agent(self, icon, item):
        self.agent.stop()
        time.sleep(1)
        self.agent = CaptureAgent(os.getenv("AC_SERVER_URL"))
        self._start_agent()

    def _open_dashboard(self, icon, item):
        import webbrowser

        webbrowser.open(f"{self.agent.server_base_url}")

    def _exit(self, icon, item):
        self.agent.stop()
        self.icon.stop()
        os.kill(os.getpid(), signal.SIGTERM)

    def _noop(self, icon, item):
        return None


def main():
    app = TrayApp()
    app.start()


if __name__ == "__main__":
    main()
