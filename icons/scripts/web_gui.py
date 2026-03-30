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
                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #121212; color: #ffffff; text-align: center; margin: 0; padding: 20px; }
                h1 { margin-bottom: 5px; }
                h3 { color: #aaaaaa; margin-top: 5px; margin-bottom: 30px; font-weight: 300; }
                .section { margin-bottom: 40px; }
                .section-title { font-size: 24px; border-bottom: 1px solid #333; padding-bottom: 10px; margin-bottom: 20px; display: inline-block; }
                .grid { display: flex; flex-wrap: wrap; justify-content: center; gap: 20px; }
                .card { background-color: #1e1e1e; border-radius: 12px; padding: 15px; cursor: pointer; transition: transform 0.2s, background-color 0.2s; border: 2px solid transparent; width: 200px; }
                .card:hover { transform: scale(1.05); background-color: #2a2a2a; border-color: #4CAF50; }
                .card img { max-width: 100%; border-radius: 8px; margin-bottom: 10px; aspect-ratio: 1/1; object-fit: cover; }
                .card span { display: block; font-size: 14px; font-weight: bold; color: #ddd; }
                .upload-btn { background-color: #4CAF50; color: white; padding: 15px 30px; border: none; border-radius: 8px; font-size: 16px; cursor: pointer; font-weight: bold; margin-top: 10px; transition: background 0.2s; }
                .upload-btn:hover { background-color: #45a049; }
                #customFile { display: none; }
                .upload-section { background-color: #1e1e1e; padding: 30px; border-radius: 12px; max-width: 500px; margin: 0 auto; border: 1px dashed #555; }
            </style>
        </head>
        <body>
            <h1>📦 Interactive Icon Selector</h1>
            <h3>Choose the primary artwork for the APK build</h3>
        """

        # SteamGridDB Section
        if ctx.get("sgdb"):
            html += '<div class="section"><div class="section-title">Direct Matches (SteamGridDB)</div><div class="grid">'
            for i, img in enumerate(ctx["sgdb"]):
                b64 = image_to_base64(img)
                html += f'''
                <div class="card" onclick="selectImage('sgdb', {i})">
                    <img src="data:image/png;base64,{b64}" alt="SGDB">
                    <span>Option {i+1}</span>
                </div>'''
            html += '</div></div>'
            
        # IGDB Smart Covers Section
        if ctx.get("igdb"):
            html += '<div class="section"><div class="section-title">Smart Compositions (IGDB)</div><div class="grid">'
            for i, img in enumerate(ctx["igdb"]):
                b64 = image_to_base64(img)
                html += f'''
                <div class="card" onclick="selectImage('igdb', {i})">
                    <img src="data:image/png;base64,{b64}" alt="IGDB">
                    <span>Smart Cover {i+1}</span>
                </div>'''
            html += '</div></div>'
            
        # Fallbacks Section
        html += '<div class="section"><div class="section-title">Local Generators (Fallbacks)</div><div class="grid">'
        if ctx.get("console"):
            b64 = image_to_base64(ctx["console"])
            html += f'''
            <div class="card" onclick="selectImage('console', 0)">
                <img src="data:image/png;base64,{b64}" alt="Console">
                <span>Default Console</span>
            </div>'''
        if ctx.get("typo"):
            b64 = image_to_base64(ctx["typo"])
            html += f'''
            <div class="card" onclick="selectImage('typo', 0)">
                <img src="data:image/png;base64,{b64}" alt="Typo">
                <span>Custom Typography</span>
            </div>'''
        html += '</div></div>'
        
        # Upload Section
        html += '''
        <div class="section upload-section">
            <div class="section-title" style="border:none; margin-bottom:0;">None of these look good?</div>
            <p style="color:#aaa; font-size:14px;">Upload your own image (Square PNG/JPG recommended)</p>
            <input type="file" id="customFile" accept="image/*" onchange="uploadImage()">
            <button class="upload-btn" onclick="document.getElementById('customFile').click()">📤 Upload Local File</button>
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
