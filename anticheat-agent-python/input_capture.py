from collections import deque
from concurrent.futures import ThreadPoolExecutor, as_completed
import ctypes
import hashlib
import os
import platform
import socket
import threading
import time
import uuid
from pathlib import Path

import requests
from pynput import keyboard, mouse
try:
    from PIL import ImageGrab
except Exception:
    ImageGrab = None

_IS_WINDOWS = platform.system().lower().startswith("win")
_USER32 = ctypes.windll.user32 if _IS_WINDOWS else None
_KERNEL32 = ctypes.windll.kernel32 if _IS_WINDOWS else None
_PROCESS_QUERY_LIMITED_INFORMATION = 0x1000
_GAME_HINT_KEYWORDS = (
    "counter-strike", "cs ", "cs2", "half-life", "valorant", "dota", "league",
    "fortnite", "apex", "overwatch", "call of duty", "cod", "pubg", "gta",
    "battlefield", "rainbow six", "siege", "minecraft", "rocket league", "steam"
)
_NON_GAME_TITLE_KEYWORDS = (
    "visual studio", "code", "notepad", "word", "excel", "powerpoint",
    "chrome", "edge", "firefox", "terminal", "powershell", "cmd", "explorer",
    "discord", "slack", "teams", "zoom", "postman", "figma",
    "aegis ai", "anti-cheat engine", "security terminal", "live overwatch"
)


class _POINT(ctypes.Structure):
    _fields_ = [("x", ctypes.c_long), ("y", ctypes.c_long)]


class _RECT(ctypes.Structure):
    _fields_ = [
        ("left", ctypes.c_long),
        ("top", ctypes.c_long),
        ("right", ctypes.c_long),
        ("bottom", ctypes.c_long),
    ]


def _get_cursor_pos() -> tuple[int, int] | None:
    if not _IS_WINDOWS or _USER32 is None:
        return None
    point = _POINT()
    if _USER32.GetCursorPos(ctypes.byref(point)) == 0:
        return None
    return int(point.x), int(point.y)


def _is_key_down(vk_code: int) -> bool:
    if not _IS_WINDOWS or _USER32 is None:
        return False
    return (_USER32.GetAsyncKeyState(vk_code) & 0x8000) != 0


def _get_process_exe_name(pid: int) -> str:
    if not _IS_WINDOWS or _KERNEL32 is None or pid <= 0:
        return ""
    handle = _KERNEL32.OpenProcess(_PROCESS_QUERY_LIMITED_INFORMATION, False, int(pid))
    if not handle:
        return ""
    try:
        size = ctypes.c_ulong(1024)
        buffer = ctypes.create_unicode_buffer(1024)
        ok = _KERNEL32.QueryFullProcessImageNameW(handle, 0, buffer, ctypes.byref(size))
        if not ok:
            return ""
        return os.path.basename(buffer.value).lower()
    except Exception:
        return ""
    finally:
        _KERNEL32.CloseHandle(handle)


def _get_foreground_window_info() -> dict | None:
    if not _IS_WINDOWS or _USER32 is None:
        return None
    hwnd = _USER32.GetForegroundWindow()
    if not hwnd:
        return None

    pid = ctypes.c_ulong(0)
    _USER32.GetWindowThreadProcessId(hwnd, ctypes.byref(pid))
    rect = _RECT()
    if _USER32.GetWindowRect(hwnd, ctypes.byref(rect)) == 0:
        return None

    title_buffer = ctypes.create_unicode_buffer(256)
    _USER32.GetWindowTextW(hwnd, title_buffer, len(title_buffer))
    title = title_buffer.value.strip()
    width = max(0, int(rect.right - rect.left))
    height = max(0, int(rect.bottom - rect.top))
    center_x = int(rect.left + (width / 2))
    center_y = int(rect.top + (height / 2))

    return {
        "pid": int(pid.value),
        "exe": _get_process_exe_name(int(pid.value)),
        "title": title,
        "left": int(rect.left),
        "top": int(rect.top),
        "right": int(rect.right),
        "bottom": int(rect.bottom),
        "width": width,
        "height": height,
        "center_x": center_x,
        "center_y": center_y,
    }


def _get_primary_screen_size() -> tuple[int, int]:
    if not _IS_WINDOWS or _USER32 is None:
        return (0, 0)
    width = int(_USER32.GetSystemMetrics(0))
    height = int(_USER32.GetSystemMetrics(1))
    return (max(0, width), max(0, height))


def _build_device_id() -> str:
    override = os.getenv("AC_DEVICE_ID", "").strip()
    if override:
        return override

    path = _device_id_path()
    saved = _read_device_id(path)
    if saved:
        return saved

    generated = uuid.uuid4().hex[:16]
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(generated, encoding="utf-8")
        return generated
    except Exception:
        machine_guid = _windows_machine_guid()
        if machine_guid:
            raw = f"{machine_guid}-{platform.node()}-{platform.machine()}"
            return hashlib.sha256(raw.encode("utf-8")).hexdigest()[:16]
        raw = f"{platform.node()}-{uuid.getnode()}-{platform.machine()}-{platform.processor()}"
        return hashlib.sha256(raw.encode("utf-8")).hexdigest()[:16]


def _device_id_path() -> Path:
    appdata = os.getenv("APPDATA")
    if appdata:
        return Path(appdata) / "AEGIS" / "device.id"
    return Path.home() / ".aegis" / "device.id"


def _server_url_path() -> Path:
    appdata = os.getenv("APPDATA")
    if appdata:
        return Path(appdata) / "AEGIS" / "server.url"
    return Path.home() / ".aegis" / "server.url"


def _read_device_id(path: Path) -> str:
    try:
        if not path.exists():
            return ""
        value = path.read_text(encoding="utf-8").strip()
        if not value:
            return ""
        return "".join(ch for ch in value if ch.isalnum() or ch in "._-")[:64]
    except Exception:
        return ""


def _read_cached_server_url(path: Path) -> str:
    try:
        if not path.exists():
            return ""
        value = path.read_text(encoding="utf-8").strip().rstrip("/")
        return value
    except Exception:
        return ""


def _write_cached_server_url(path: Path, url: str) -> None:
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(url.rstrip("/"), encoding="utf-8")
    except Exception:
        pass


def _windows_machine_guid() -> str:
    try:
        import winreg  # type: ignore

        with winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, r"SOFTWARE\Microsoft\Cryptography") as key:
            value, _ = winreg.QueryValueEx(key, "MachineGuid")
            return str(value).strip()
    except Exception:
        return ""


def _build_metadata(device_id: str) -> dict:
    return {
        "deviceId": device_id,
        "hostname": platform.node() or "unknown",
        "os": platform.platform(),
        "appVersion": "1.0.0",
        "ip": _get_local_ip(),
    }


def _get_local_ip() -> str:
    try:
        # Uses default route interface; avoids picking virtual adapters in most setups.
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
            s.connect(("8.8.8.8", 80))
            return s.getsockname()[0]
    except Exception:
        try:
            return socket.gethostbyname(socket.gethostname())
        except Exception:
            return "unknown"


def _is_server_reachable(base_url: str, timeout: float = 0.8) -> bool:
    try:
        r = requests.get(f"{base_url.rstrip('/')}/api/health", timeout=timeout)
        return r.status_code < 500
    except Exception:
        return False


def _discover_server_on_lan(port: int = 8080, timeout: float = 0.35) -> str:
    local_ip = _get_local_ip()
    if local_ip == "unknown" or "." not in local_ip:
        return ""

    octets = local_ip.split(".")
    if len(octets) != 4:
        return ""
    prefix = ".".join(octets[:3])

    def probe(host: str) -> str:
        url = f"http://{host}:{port}"
        if _is_server_reachable(url, timeout=timeout):
            return url
        return ""

    candidates = [f"{prefix}.{i}" for i in range(1, 255) if f"{prefix}.{i}" != local_ip]
    with ThreadPoolExecutor(max_workers=64) as ex:
        futures = {ex.submit(probe, host): host for host in candidates}
        for future in as_completed(futures):
            found = future.result()
            if found:
                return found
    return ""


def _resolve_server_url(initial_url: str) -> str:
    normalized_initial = (initial_url or "http://localhost:8080").rstrip("/")
    cache_path = _server_url_path()
    cached = _read_cached_server_url(cache_path)

    candidates = []
    for c in [normalized_initial, cached, "http://localhost:8080"]:
        c = (c or "").rstrip("/")
        if c and c not in candidates:
            candidates.append(c)

    for candidate in candidates:
        if _is_server_reachable(candidate):
            _write_cached_server_url(cache_path, candidate)
            return candidate

    discovered = _discover_server_on_lan()
    if discovered:
        _write_cached_server_url(cache_path, discovered)
        return discovered

    return normalized_initial


class CaptureAgent:
    def __init__(self, server_base_url: str | None = None):
        base = server_base_url or os.getenv("AC_SERVER_URL", "http://localhost:8080")
        self.server_base_url = _resolve_server_url(base)
        self._rebuild_urls()

        self.device_id = _build_device_id()
        self.metadata = _build_metadata(self.device_id)

        self.mouse_events = deque(maxlen=500)
        self.key_events = deque(maxlen=200)
        self.running = False
        self.last_mouse_time = 0.0

        self.send_interval = 0.5
        self.heartbeat_interval = 5.0
        self.mouse_sample_rate = 0.02

        self._sender_thread = None
        self._heartbeat_thread = None
        self._mouse_listener = None
        self._keyboard_listener = None
        self._poller_thread = None
        self.poll_interval = 0.016
        self.idle_poll_interval = 0.05
        self._last_polled_pos = None
        self._tracked_mouse_buttons = {
            0x01: "Button.left",
            0x02: "Button.right",
            0x04: "Button.middle",
        }
        self._tracked_keys = {
            0x57: "w",
            0x41: "a",
            0x53: "s",
            0x44: "d",
            0x20: "space",
            0x10: "shift",
            0x11: "ctrl",
            0x12: "alt",
            0x52: "r",
            0x31: "1",
            0x32: "2",
            0x33: "3",
            0x34: "4",
            0x35: "5",
        }
        self._mouse_button_state = {vk: False for vk in self._tracked_mouse_buttons}
        self._key_state = {vk: False for vk in self._tracked_keys}
        self.target_games = self._resolve_target_games()
        self.excluded_processes = self._resolve_excluded_processes()
        self.game_active = not _IS_WINDOWS
        self.game_exe = "unknown"
        self.game_title = ""
        self.game_center = None
        self._last_game_pos = None
        self._last_context_refresh = 0.0
        self.context_refresh_interval = 0.2
        self._screen_size = _get_primary_screen_size()
        self.screen_capture_interval = 0.22
        self._last_screen_capture = 0.0
        self._prev_screen_signature = None
        self._screen_motion_samples = deque(maxlen=10)
        self._last_screen_motion = 0.0
        self._last_sent_game_active = None
        self._last_state_ping = 0.0
        self.state_ping_interval = 1.5

    def _rebuild_urls(self):
        self.ingest_url = f"{self.server_base_url}/api/ingest"
        self.register_url = f"{self.server_base_url}/api/agents/register"
        self.heartbeat_url = f"{self.server_base_url}/api/agents/heartbeat"

    def _recover_server(self):
        previous = self.server_base_url
        resolved = _resolve_server_url(previous)
        if resolved != previous:
            self.server_base_url = resolved
            self._rebuild_urls()
            print(f"[agent] switched server to: {self.server_base_url}")

    def _resolve_target_games(self) -> set[str]:
        raw = os.getenv("AC_GAME_EXE_NAMES", "")
        return {item.strip().lower() for item in raw.split(",") if item.strip()}

    def _resolve_excluded_processes(self) -> set[str]:
        raw = os.getenv(
            "AC_EXCLUDED_EXE_NAMES",
            "explorer.exe,chrome.exe,msedge.exe,firefox.exe,code.exe,powershell.exe,cmd.exe,devenv.exe,obs64.exe,discord.exe,slack.exe,teams.exe,notepad.exe,winword.exe,excel.exe,powerpnt.exe,java.exe,javaw.exe",
        )
        return {item.strip().lower() for item in raw.split(",") if item.strip()}

    def _is_large_window(self, info: dict) -> bool:
        screen_w, screen_h = self._screen_size
        if screen_w <= 0 or screen_h <= 0:
            return False
        width = int(info.get("width", 0))
        height = int(info.get("height", 0))
        area_ratio = (width * height) / max(1, (screen_w * screen_h))
        width_ratio = width / max(1, screen_w)
        height_ratio = height / max(1, screen_h)
        return area_ratio >= 0.55 or (width_ratio >= 0.82 and height_ratio >= 0.72)

    def _has_game_hint(self, exe: str, title: str) -> bool:
        hay = f"{exe} {title}".lower()
        return any(keyword in hay for keyword in _GAME_HINT_KEYWORDS)

    def _looks_productivity_title(self, title: str) -> bool:
        t = (title or "").lower()
        return any(keyword in t for keyword in _NON_GAME_TITLE_KEYWORDS)

    def _looks_like_game_window(self, info: dict, exe: str) -> bool:
        width = int(info.get("width", 0))
        height = int(info.get("height", 0))
        if width < 640 or height < 480:
            return False
        if exe in self.excluded_processes:
            return False
        title = str(info.get("title") or "")
        # Auto-detect mode by default; if target list is provided, use it as allow-list.
        if self.target_games:
            return exe in self.target_games
        if self._looks_productivity_title(title):
            return False
        if self._has_game_hint(exe, title):
            return True
        return self._is_large_window(info)

    def _refresh_game_context(self, force: bool = False):
        now = time.time()
        if not force and (now - self._last_context_refresh) < self.context_refresh_interval:
            return
        self._last_context_refresh = now

        if not _IS_WINDOWS:
            self.game_active = True
            return

        info = _get_foreground_window_info()
        if not info:
            self.game_active = False
            self.game_exe = "unknown"
            self.game_title = ""
            self.game_center = None
            self._last_game_pos = None
            self._prev_screen_signature = None
            self._screen_motion_samples.clear()
            self._last_screen_motion = 0.0
            return

        exe = str(info.get("exe") or "").lower()
        title = str(info.get("title") or "")
        if self._looks_like_game_window(info, exe):
            self.game_active = True
            self.game_exe = exe or "unknown"
            self.game_title = title
            self.game_center = (int(info["center_x"]), int(info["center_y"]))
        else:
            self.game_active = False
            self.game_exe = exe or "unknown"
            self.game_title = title
            self.game_center = None
            self._last_game_pos = None
            self._prev_screen_signature = None
            self._screen_motion_samples.clear()
            self._last_screen_motion = 0.0

    def _append_mouse_event(self, x: int, y: int, now: float, button: str | None = None, pressed: bool | None = None):
        event = {"x": x, "y": y, "t": now}
        if button is not None:
            event["button"] = button
        if pressed is not None:
            event["pressed"] = pressed
        if self.game_center is not None:
            event["cx"] = int(x - self.game_center[0])
            event["cy"] = int(y - self.game_center[1])
        if self._last_game_pos is not None:
            event["dx"] = int(x - self._last_game_pos[0])
            event["dy"] = int(y - self._last_game_pos[1])
        self._last_game_pos = (x, y)
        self.mouse_events.append(event)

    def _capture_screen_motion(self, now: float):
        if ImageGrab is None:
            return
        if not _IS_WINDOWS or not self.game_active or self.game_center is None:
            return
        if (now - self._last_screen_capture) < self.screen_capture_interval:
            return
        self._last_screen_capture = now

        center_x, center_y = self.game_center
        half_size = 48
        bbox = (
            max(0, center_x - half_size),
            max(0, center_y - half_size),
            max(1, center_x + half_size),
            max(1, center_y + half_size),
        )
        try:
            frame = ImageGrab.grab(bbox=bbox, all_screens=True).convert("L").resize((20, 20))
            signature = frame.tobytes()
            motion = 0.0
            if self._prev_screen_signature is not None:
                diff_sum = 0
                prev = self._prev_screen_signature
                for i in range(len(signature)):
                    diff_sum += abs(signature[i] - prev[i])
                motion = diff_sum / len(signature)
            self._prev_screen_signature = signature
            self._screen_motion_samples.append(motion)
            if self._screen_motion_samples:
                self._last_screen_motion = sum(self._screen_motion_samples) / len(self._screen_motion_samples)
        except Exception:
            pass

    def start(self):
        if self.running:
            return

        self.running = True
        self._refresh_game_context(force=True)
        self._register()

        self._sender_thread = threading.Thread(target=self._sender_loop, daemon=True)
        self._sender_thread.start()

        self._heartbeat_thread = threading.Thread(target=self._heartbeat_loop, daemon=True)
        self._heartbeat_thread.start()

        self._mouse_listener = mouse.Listener(on_move=self._on_move, on_click=self._on_click)
        self._keyboard_listener = keyboard.Listener(on_press=self._on_press, on_release=self._on_release)
        self._mouse_listener.start()
        self._keyboard_listener.start()

        if _IS_WINDOWS:
            self._poller_thread = threading.Thread(target=self._windows_poll_loop, daemon=True)
            self._poller_thread.start()

    def wait(self):
        if self._keyboard_listener is not None:
            self._keyboard_listener.join()

    def stop(self):
        self.running = False
        if self._mouse_listener is not None:
            self._mouse_listener.stop()
        if self._keyboard_listener is not None:
            self._keyboard_listener.stop()

    def _register(self):
        try:
            res = requests.post(self.register_url, json=self.metadata, timeout=2.5)
            if res.status_code >= 400:
                print(f"[agent] register failed: {res.status_code}")
        except Exception as exc:
            print(f"[agent] register error: {exc}")
            self._recover_server()

    def _heartbeat_loop(self):
        while self.running:
            try:
                res = requests.post(self.heartbeat_url, json=self.metadata, timeout=2.5)
                if res.status_code >= 400:
                    print(f"[agent] heartbeat failed: {res.status_code}")
            except Exception as exc:
                print(f"[agent] heartbeat error: {exc}")
                self._recover_server()
            time.sleep(self.heartbeat_interval)

    def _sender_loop(self):
        while self.running:
            time.sleep(self.send_interval)
            self._refresh_game_context()
            now = time.time()
            self._capture_screen_motion(now)
            has_screen_signal = self._last_screen_motion > 0 and self.game_active
            game_state_changed = self._last_sent_game_active is None or self._last_sent_game_active != self.game_active
            ping_due = (now - self._last_state_ping) >= self.state_ping_interval
            should_send_state = game_state_changed or ping_due
            if not self.mouse_events and not self.key_events and not has_screen_signal and not should_send_state:
                continue

            payload = {
                **self.metadata,
                "mouse": list(self.mouse_events),
                "keys": list(self.key_events),
                "game": {
                    "active": self.game_active,
                    "exe": self.game_exe,
                    "title": self.game_title,
                },
                "screen": {
                    "motion": self._last_screen_motion,
                    "sampleCount": len(self._screen_motion_samples),
                },
                "timestamp": now,
            }
            self.mouse_events.clear()
            self.key_events.clear()

            try:
                res = requests.post(self.ingest_url, json=payload, timeout=2.5)
                if res.status_code >= 400:
                    print(f"[agent] ingest failed: {res.status_code}")
                else:
                    self._last_sent_game_active = self.game_active
                    self._last_state_ping = now
            except Exception as exc:
                print(f"[agent] ingest error: {exc}")
                self._recover_server()

    def _on_move(self, x, y):
        self._refresh_game_context()
        if not self.game_active:
            return
        now = time.time()
        if now - self.last_mouse_time < self.mouse_sample_rate:
            return
        self.last_mouse_time = now
        self._append_mouse_event(int(x), int(y), now)

    def _on_click(self, x, y, button, pressed):
        self._refresh_game_context()
        if not self.game_active:
            return
        self._append_mouse_event(int(x), int(y), time.time(), button=str(button), pressed=bool(pressed))

    def _on_press(self, key):
        self._refresh_game_context()
        if not self.game_active:
            return
        self.key_events.append({"key": str(key), "t": time.time()})

    def _on_release(self, key):
        if key == keyboard.Key.esc:
            self.stop()
            print("Capture stopped (ESC)")
            return False
        return None

    def _windows_poll_loop(self):
        # Fallback polling path for games where global hook callbacks are suppressed.
        while self.running:
            now = time.time()
            self._refresh_game_context()
            self._capture_screen_motion(now)
            if not self.game_active:
                # When no game is active, avoid expensive keyboard/mouse polling loops.
                time.sleep(self.idle_poll_interval)
                continue

            pos = _get_cursor_pos()
            if pos is not None and pos != self._last_polled_pos:
                self._last_polled_pos = pos
                self._append_mouse_event(int(pos[0]), int(pos[1]), now)

            for vk, name in self._tracked_mouse_buttons.items():
                current = _is_key_down(vk)
                previous = self._mouse_button_state[vk]
                if current != previous:
                    self._mouse_button_state[vk] = current
                    x, y = self._last_polled_pos if self._last_polled_pos is not None else (0, 0)
                    self._append_mouse_event(int(x), int(y), now, button=name, pressed=bool(current))

            for vk, name in self._tracked_keys.items():
                current = _is_key_down(vk)
                previous = self._key_state[vk]
                if current and not previous:
                    self.key_events.append({"key": name, "t": now})
                self._key_state[vk] = current

            time.sleep(self.poll_interval)


def main():
    agent = CaptureAgent()
    print(f"Input capture started for device {agent.device_id} (ESC to stop)")
    print(f"Server: {agent.server_base_url}")
    print(f"Metadata: {agent.metadata}")
    if agent.target_games:
        print(f"Target games: {sorted(agent.target_games)}")
    else:
        print("Target games: auto-detect mode (set AC_GAME_EXE_NAMES to pin specific games)")
    if _IS_WINDOWS:
        print("Capture mode: foreground game-gated hooks + Windows fallback polling")
        if ImageGrab is None:
            print("Screen motion: disabled (Pillow not installed)")
        else:
            print("Screen motion: enabled")
    else:
        print("Capture mode: pynput hooks")
    agent.start()
    agent.wait()


if __name__ == "__main__":
    main()
