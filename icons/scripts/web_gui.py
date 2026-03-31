import http.server
import socketserver
import threading
import webbrowser
import base64
import json
import socket
from io import BytesIO
from PIL import Image

def image_to_base64(img):
    if not img:
        return ""
    buffered = BytesIO()
    # Pega o resize rápido apenas para a vitrine na web (para não pesar os MBs)
    img_copy = img.copy()
    img_copy.thumbnail((256, 256))
    img_copy.save(buffered, format="PNG")
    return base64.b64encode(buffered.getvalue()).decode('utf-8')

def find_free_port():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("", 0))
        return s.getsockname()[1]

selected_image = None
server_instance = None

class IconPickerHandler(http.server.BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        pass # Suppress HTTP logs to keep terminal clean
        
    def do_GET(self):
        ctx = self.server.context
        
        html = '''
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Revenger Icon Picker</title>
            <style>
                body { 
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    background-color: #161618; 
                    color: #e0e0e0; 
                    margin: 0;
                    display: flex;
                    height: 100vh;
                    overflow: hidden;
                }
                
                .sidebar {
                    width: 260px;
                    background-color: #1e1e21;
                    padding: 30px 20px;
                    display: flex;
                    flex-direction: column;
                    gap: 15px;
                    border-right: 1px solid #2a2a2d;
                    box-sizing: border-box;
                    flex-shrink: 0;
                    z-index: 10;
                    box-shadow: 2px 0 8px rgba(0,0,0,0.2);
                }
                
                .header-container {
                    margin-bottom: 20px;
                }
                
                h1 { color: #ffffff; margin: 0 0 8px 0; font-size: 22px; line-height: 1.2; }
                h3 { color: #999999; margin: 0; font-weight: normal; font-size: 14px; line-height: 1.4; }
                
                .menu-item {
                    background-color: transparent;
                    color: #b3b3b3;
                    border: none;
                    padding: 12px 16px;
                    font-size: 15px;
                    font-weight: 500;
                    border-radius: 8px;
                    cursor: pointer;
                    transition: all 0.2s;
                    display: flex;
                    align-items: center;
                    justify-content: flex-start;
                    gap: 12px;
                    width: 100%;
                    box-sizing: border-box;
                    text-align: left;
                }
                .menu-item:hover { 
                    background-color: rgba(255, 255, 255, 0.08); 
                    color: #ffffff; 
                }
                .menu-item.danger { 
                    margin-top: auto; 
                }
                .menu-item.danger:hover { 
                    background-color: rgba(255, 77, 77, 0.15); 
                    color: #ff6666; 
                }
                .menu-item.primary { 
                    color: #b3b3b3; 
                }
                .menu-item.primary:hover { 
                    background-color: rgba(100, 160, 255, 0.15); 
                    color: #99c2ff; 
                }
                
                .main-content {
                    flex-grow: 1;
                    overflow-y: auto;
                    padding: 10px 40px 60px 40px;
                    box-sizing: border-box;
                }
                
                .section { margin-top: 30px; text-align: left; max-width: 1050px; margin-left: auto; margin-right: auto;}
                .section-title { font-size: 18px; color: #ececec; font-weight: 600; border-bottom: 1px solid #333; padding-bottom: 10px; margin-bottom: 20px; padding-left: 5px;}
                .grid { display: flex; flex-wrap: wrap; justify-content: flex-start; gap: 20px; }
                @media (max-width: 1100px) {
                    .grid { justify-content: center; }
                }
                @media (max-width: 768px) {
                    body { flex-direction: column; height: auto; overflow: auto; }
                    .sidebar { width: 100%; height: auto; border-right: none; border-bottom: 1px solid #2a2a2d; box-shadow: 0 2px 8px rgba(0,0,0,0.2); }
                    .menu-item.danger { margin-top: 0; }
                    .main-content { padding: 20px; overflow-y: visible; }
                }
                
                .card { 
                    background-color: #1e1e21; 
                    border-radius: 12px; 
                    padding: 16px; 
                    cursor: pointer; 
                    transition: transform 0.2s cubic-bezier(0.2, 0, 0, 1), background-color 0.2s; 
                    border: 1px solid #2a2a2d;
                    width: calc(33.333% - 20px);
                    min-width: 240px;
                    max-width: 280px;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    box-sizing: border-box;
                }
                .card:hover { 
                    background-color: #27272b; 
                    transform: translateY(-4px); 
                    border-color: #444;
                    box-shadow: 0 6px 12px rgba(0,0,0,0.2);
                }
                .card > span { display: block; font-size: 14px; margin-top: 15px; font-weight: 600; color: #ececec; }
                .card .resolution { font-size: 12px; color: #888; margin-top: 6px; font-weight: normal; }
                
                .preview-container {
                    display: flex;
                    justify-content: space-around;
                    width: 100%;
                    gap: 10px;
                }
                .icon-preview {
                    width: 64px;
                    height: 64px;
                    position: relative;
                    background-color: #0b0b0c;
                    border: 1px solid #333;
                }
                .icon-preview.legacy-sq { border-radius: 12px; overflow: hidden; }
                .icon-preview.legacy-round { border-radius: 50%; overflow: hidden; }
                .icon-preview.adaptive { border-radius: 14px; border: none; background-color: #222; }
                
                .img-full { width: 100%; height: 100%; object-fit: contain; }
                .bg-blur {
                    position: absolute;
                    top: -10%; left: -10%; width: 120%; height: 120%;
                    object-fit: cover;
                    filter: blur(4px) brightness(0.6);
                }
                .fg-safe {
                    position: absolute;
                    top: 16%; left: 16%; width: 68%; height: 68%;
                    object-fit: contain;
                    z-index: 2;
                    filter: drop-shadow(0px 3px 4px rgba(0,0,0,0.7));
                }
                #customFile { display: none; }
            </style>
        </head>
        <body>
            <div class="sidebar">
                <div class="header-container">
                    <h1>Icon Selector</h1>
                    <h3>Choose the primary artwork for the APK build</h3>
                </div>
                
                <input type="file" id="customFile" accept="image/*" onchange="uploadImage()">
                <button class="menu-item primary" onclick="document.getElementById('customFile').click()">📤 Upload Local File</button>
                <button class="menu-item" onclick="clearOverride()">🔄 Restore Auto-Scraping</button>
                <button class="menu-item danger" onclick="cancelProcess()">❌ Cancel & Close</button>
            </div>
            
            <div class="main-content">
'''
        # SteamGridDB Section
        if ctx.get("sgdb"):
            html += '<div class="section"><div class="section-title">Direct Matches (SGDB)</div><div class="grid">'
            for i, img in enumerate(ctx["sgdb"]):
                b64 = image_to_base64(img)
                w, h = img.size
                html += f'''
                <div class="card" onclick="selectImage('sgdb', {i})">
                    <div class="preview-container">
                        <div class="icon-preview legacy-sq" title="Legacy Square">
                            <img class="img-full" src="data:image/png;base64,{b64}">
                        </div>
                        <div class="icon-preview legacy-round" title="Legacy Round">
                            <img class="img-full" src="data:image/png;base64,{b64}">
                        </div>
                        <div class="icon-preview adaptive" title="Adaptive Parallax">
                            <img class="bg-blur" src="data:image/png;base64,{b64}">
                            <img class="fg-safe" src="data:image/png;base64,{b64}">
                        </div>
                    </div>
                    <span>Option {i+1}</span>
                    <span class="resolution">{w}x{h}</span>
                </div>'''
            html += '</div></div>'
        
        # IGDB Smart Covers Section
        if ctx.get("igdb"):
            html += '<div class="section"><div class="section-title">Smart Compositions (IGDB)</div><div class="grid">'
            for i, img in enumerate(ctx["igdb"]):
                b64 = image_to_base64(img)
                w, h = img.size
                html += f'''
                <div class="card" onclick="selectImage('igdb', {i})">
                    <div class="preview-container">
                        <div class="icon-preview legacy-sq" title="Legacy Square">
                            <img class="img-full" src="data:image/png;base64,{b64}">
                        </div>
                        <div class="icon-preview legacy-round" title="Legacy Round">
                            <img class="img-full" src="data:image/png;base64,{b64}">
                        </div>
                        <div class="icon-preview adaptive" title="Adaptive Parallax">
                            <img class="bg-blur" src="data:image/png;base64,{b64}">
                            <img class="fg-safe" src="data:image/png;base64,{b64}">
                        </div>
                    </div>
                    <span>Smart Cover {i+1}</span>
                    <span class="resolution">{w}x{h}</span>
                </div>'''
            html += '</div></div>'

        # Fallbacks Section
        html += '<div class="section"><div class="section-title">Local Fallbacks</div><div class="grid">'
        if ctx.get("console"):
            b64 = image_to_base64(ctx["console"])
            w, h = ctx["console"].size
            html += f'''
            <div class="card" onclick="selectImage('console', 0)">
                <div class="preview-container">
                    <div class="icon-preview legacy-sq" title="Legacy Square">
                        <img class="img-full" src="data:image/png;base64,{b64}">
                    </div>
                    <div class="icon-preview legacy-round" title="Legacy Round">
                        <img class="img-full" src="data:image/png;base64,{b64}">
                    </div>
                    <div class="icon-preview adaptive" title="Adaptive Parallax">
                        <img class="bg-blur" src="data:image/png;base64,{b64}">
                        <img class="fg-safe" src="data:image/png;base64,{b64}">
                    </div>
                </div>
                <span>Default Console</span>
                <span class="resolution">{w}x{h}</span>
            </div>'''
        if ctx.get("typo"):
            b64 = image_to_base64(ctx["typo"])
            w, h = ctx["typo"].size
            html += f'''
            <div class="card" onclick="selectImage('typo', 0)">
                <div class="preview-container">
                    <div class="icon-preview legacy-sq" title="Legacy Square">
                        <img class="img-full" src="data:image/png;base64,{b64}">
                    </div>
                    <div class="icon-preview legacy-round" title="Legacy Round">
                        <img class="img-full" src="data:image/png;base64,{b64}">
                    </div>
                    <div class="icon-preview adaptive" title="Adaptive Parallax">
                        <img class="bg-blur" src="data:image/png;base64,{b64}">
                        <img class="fg-safe" src="data:image/png;base64,{b64}">
                    </div>
                </div>
                <span>Custom Typography</span>
                <span class="resolution">{w}x{h}</span>
            </div>'''
        html += '</div></div>'

        # Javascript interactions
        html += '''
            </div>
            <script>
                async function selectImage(group, index) {
                    document.body.innerHTML = "<h2 style='margin-top: 100px; color: #4CAF50;'>⏳ Processing selected icon...</h2><p>You can now close this tab and return to the terminal.</p>";
                    await fetch('/select', {
                        method: 'POST',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify({group: group, index: index})
                    });
                }
                
                function uploadImage() {
                    const file = document.getElementById('customFile').files[0];
                    if (!file) return;
                    const reader = new FileReader();
                    document.body.innerHTML = "<h2 style='margin-top: 100px; color: #4CAF50;'>⏳ Please wait, processing local image...</h2>";
                    reader.onload = async function(e) {
                        const b64 = e.target.result;
                        await fetch('/select', {
                            method: 'POST',
                            headers: {'Content-Type': 'application/json'},
                            body: JSON.stringify({custom_base64: b64})
                        });
                        document.body.innerHTML = "<h2 style='margin-top: 100px; color: #4CAF50;'>✅ Icon captured!</h2><p>You can now close this tab and return to the terminal.</p>";
                    };
                    reader.readAsDataURL(file);
                }

                async function clearOverride() {
                    document.body.innerHTML = "<h2 style='margin-top: 100px; color: #f44336;'>🗑️ Reverting to automatic behavior...</h2><p>You can now close this tab and return to the terminal.</p>";
                    await fetch('/select', {
                        method: 'POST',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify({action: 'clear_override'})
                    });
                }

                async function cancelProcess() {
                    document.body.innerHTML = "<h2 style='margin-top: 100px; color: #aaaaaa;'>⏹️ Process cancelled.</h2><p>No changes were made. You can safely close this tab.</p>";
                    await fetch('/select', {
                        method: 'POST',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify({action: 'cancel'})
                    });
                }
            </script>
        </body>
        </html>
        '''

        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()
        self.wfile.write(html.encode("utf-8"))
        
    def do_POST(self):
        global selected_image, server_instance
        if self.path == '/select':
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            data = json.loads(post_data.decode('utf-8'))
            
            if "custom_base64" in data:
                b64_data = data["custom_base64"].split(",")[1]
                selected_image = Image.open(BytesIO(base64.b64decode(b64_data))).convert("RGBA")
            elif "action" in data and data["action"] == "clear_override":
                selected_image = "CLEAR_OVERRIDE"
            elif "action" in data and data["action"] == "cancel":
                selected_image = "CANCEL"
            else:
                group = data.get("group")
                index = int(data.get("index"))
                if group in self.server.context:
                    if isinstance(self.server.context[group], list):
                        selected_image = self.server.context[group][index]
                    else:
                        selected_image = self.server.context[group]
                        
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(b'{"status":"success"}')
            
            # Encerrar o servidor num background thread logo após a resposta
            threading.Thread(target=server_instance.shutdown, daemon=True).start()


def start_web_picker(context_dict):
    """
    Inicia o servidor e bloqueia a thread principal até uma escolha ser feita.
    context_dict possuirá as chaves: 'sgdb' (lista), 'igdb' (lista), 'console' (img), 'typo' (img)
    """
    global selected_image, server_instance
    selected_image = None
    
    port = find_free_port()
    
    class WebPickerServer(http.server.HTTPServer):
        def __init__(self, server_address, RequestHandlerClass, context):
            super().__init__(server_address, RequestHandlerClass)
            self.context = context
            
    server_instance = WebPickerServer(("", port), IconPickerHandler, context_dict)
    
    print(f"\n🌐 [Web UI] Local server started at http://localhost:{port}")
    print("🌐 The browser should open automatically. If not, click the link above.")
    print("⏳ Waiting for developer's decision in the browser...")
    print("⌨️  Press Ctrl+C in this terminal to strictly abort the process without changes.\n")
    
    # Abrir navegador
    webbrowser.open(f"http://localhost:{port}")
    
    try:
        # Bloquear servidor até o .shutdown() ser chamado via do_POST
        server_instance.serve_forever()
    except KeyboardInterrupt:
        print("\n\n⏹️ [Web UI] Aborted by the user. Closing local server...")
        server_instance.server_close()
        import sys
        sys.exit(0)
    
    print("✅ [Web UI] Input received! Formatting Mipmaps...")
    return selected_image
