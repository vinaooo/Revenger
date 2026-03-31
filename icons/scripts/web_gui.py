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
        
        html = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Revenger Icon Picker</title>
            <style>
                html, body { height: 100%; margin: 0; padding: 0; box-sizing: border-box; }
                body { 
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
                    background-color: #121212; 
                    color: #ffffff; 
                    text-align: center; 
                    display: flex;
                    flex-direction: column;
                    padding: 10px 20px;
                }
                .header-area { flex-shrink: 0; }
                h1 { margin: 10px 0 5px 0; font-size: 24px; }
                h3 { color: #aaaaaa; margin-top: 5px; margin-bottom: 15px; font-weight: 300; font-size: 16px; }
                
                .main-content {
                    display: flex;
                    flex-direction: row;
                    flex-wrap: wrap;
                    justify-content: center;
                    align-items: stretch;
                    gap: 15px;
                    flex-grow: 1;
                    min-height: 0;
                }

                .section { 
                    flex: 1 1 300px;
                    display: flex;
                    flex-direction: column;
                    background-color: #1a1a1a;
                    border-radius: 12px;
                    padding: 10px;
                    box-shadow: 0 4px 6px rgba(0,0,0,0.3);
                }
                
                .section-title { 
                    font-size: 18px; 
                    border-bottom: 1px solid #333; 
                    padding-bottom: 8px; 
                    margin-bottom: 12px; 
                    font-weight: bold;
                    color: #e0e0e0;
                }
                
                .grid { 
                    display: flex; 
                    flex-wrap: wrap; 
                    justify-content: center; 
                    gap: 10px; 
                    overflow-y: auto;
                    align-content: flex-start;
                    flex-grow: 1;
                    /* Shrink scrollbar */
                    scrollbar-width: thin;
                    scrollbar-color: #555 #1a1a1a;
                }
                
                .card { 
                    background-color: #222; 
                    border-radius: 10px; 
                    padding: 8px; 
                    cursor: pointer; 
                    transition: transform 0.2s, background-color 0.2s; 
                    border: 2px solid transparent; 
                    width: calc(33% - 15px);
                    min-width: 90px;
                    max-width: 140px;
                    display: flex;
                    flex-direction: column;
                }
                
                .card:hover { transform: scale(1.05); background-color: #2a2a2a; border-color: #4CAF50; }
                .card img { width: 100%; border-radius: 6px; margin-bottom: 6px; aspect-ratio: 1/1; object-fit: cover; }
                .card span { display: block; font-size: 12px; font-weight: bold; color: #ddd; margin-top: auto; }
                
                .upload-btn { background-color: #4CAF50; color: white; padding: 10px 15px; border: none; border-radius: 8px; font-size: 14px; cursor: pointer; font-weight: bold; transition: background 0.2s; }
                .upload-btn:hover { background-color: #45a049; }
                #customFile { display: none; }
                
                .upload-section { 
                    background-color: #1e1e1e; 
                    padding: 15px; 
                    border-radius: 12px; 
                    border: 1px dashed #555; 
                    margin-top: 15px;
                    flex-shrink: 0;
                }
                
                .upload-controls {
                    display: flex; 
                    gap: 10px; 
                    justify-content: center; 
                    flex-wrap: wrap;
                    margin-top: 10px;
                }
            </style>
        </head>
        <body>
            <div class="header-area">
                <h1>📦 Interactive Icon Selector</h1>
                <h3>Choose the primary artwork for the APK build</h3>
            </div>
            
            <div class="main-content">
        """

        # SteamGridDB Section
        if ctx.get("sgdb"):
            html += '<div class="section"><div class="section-title">Direct Matches (SGDB)</div><div class="grid">'
            for i, img in enumerate(ctx["sgdb"]):
                b64 = image_to_base64(img)
                w, h = img.size
                html += f'''
                <div class="card" onclick="selectImage('sgdb', {i})">
                    <img src="data:image/png;base64,{b64}" alt="SGDB">
                    <span>Option {i+1}</span>
                    <span style="font-size: 11px; color: #888; font-weight: normal; margin-top: 4px;">{w}x{h}</span>
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
                    <img src="data:image/png;base64,{b64}" alt="IGDB">
                    <span>Smart Cover {i+1}</span>
                    <span style="font-size: 11px; color: #888; font-weight: normal; margin-top: 4px;">{w}x{h}</span>
                </div>'''
            html += '</div></div>'
            
        # Fallbacks Section
        html += '<div class="section"><div class="section-title">Local Fallbacks</div><div class="grid">'
        if ctx.get("console"):
            b64 = image_to_base64(ctx["console"])
            w, h = ctx["console"].size
            html += f'''
            <div class="card" onclick="selectImage('console', 0)">
                <img src="data:image/png;base64,{b64}" alt="Console">
                <span>Default Console</span>
                <span style="font-size: 11px; color: #888; font-weight: normal; margin-top: 4px;">{w}x{h}</span>
            </div>'''
        if ctx.get("typo"):
            b64 = image_to_base64(ctx["typo"])
            w, h = ctx["typo"].size
            html += f'''
            <div class="card" onclick="selectImage('typo', 0)">
                <img src="data:image/png;base64,{b64}" alt="Typo">
                <span>Custom Typography</span>
                <span style="font-size: 11px; color: #888; font-weight: normal; margin-top: 4px;">{w}x{h}</span>
            </div>'''
        html += '</div></div>'
        
        html += '</div>' # End main-content
        
        # Upload Section
        html += '''
        <div class="upload-section">
            <div class="section-title" style="border:none; margin-bottom:0; font-size: 16px;">None of these look good?</div>
            <input type="file" id="customFile" accept="image/*" onchange="uploadImage()">
            <div class="upload-controls">
                <button class="upload-btn" onclick="document.getElementById('customFile').click()">📤 Upload Local File (Square)</button>
                <button class="upload-btn" style="background-color: #555;" onclick="cancelProcess()">❌ Cancel & Close</button>
                <button class="upload-btn" style="background-color: #f44336;" onclick="clearOverride()">🔄 Restore Auto-Scraping</button>
            </div>
        </div>
        '''

        # Javascript interactions
        html += '''
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
