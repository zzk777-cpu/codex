#!/usr/bin/env python3
from http.server import BaseHTTPRequestHandler, HTTPServer
import json
import threading

class NLPHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path != '/analyze':
            self.send_response(404); self.end_headers(); return
        length = int(self.headers.get('Content-Length', '0'))
        body = self.rfile.read(length).decode('utf-8')
        payload = json.loads(body or '{}')
        text = f"{payload.get('title','')} {payload.get('content','')}"
        category = '论文' if '论文' in text else ('教案' if '教案' in text else '课程文档')
        resp = {
            'predicted_category': category,
            'confidence': 0.91,
            'keywords': ['课程', '文档', '智能管理']
        }
        data = json.dumps(resp, ensure_ascii=False).encode('utf-8')
        self.send_response(200)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)

class ESHandler(BaseHTTPRequestHandler):
    docs = []

    def do_POST(self):
        length = int(self.headers.get('Content-Length', '0'))
        body = self.rfile.read(length).decode('utf-8')

        if self.path.endswith('/_doc'):
            payload = json.loads(body or '{}')
            ESHandler.docs.append(payload)
            resp = {'result': 'created', '_id': str(len(ESHandler.docs))}
        elif self.path.endswith('/_search'):
            query = json.loads(body or '{}')
            q = query.get('query', {}).get('multi_match', {}).get('query', '')
            hits = []
            for idx, d in enumerate(ESHandler.docs, 1):
                content = f"{d.get('title','')} {d.get('content','')}"
                if q in content:
                    hits.append({'_id': str(idx), '_source': d})
            resp = {'hits': {'total': {'value': len(hits)}, 'hits': hits}}
        else:
            self.send_response(404); self.end_headers(); return

        data = json.dumps(resp, ensure_ascii=False).encode('utf-8')
        self.send_response(200)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)


def run_server(port, handler):
    srv = HTTPServer(('127.0.0.1', port), handler)
    srv.serve_forever()

if __name__ == '__main__':
    t1 = threading.Thread(target=run_server, args=(18000, NLPHandler), daemon=True)
    t2 = threading.Thread(target=run_server, args=(19200, ESHandler), daemon=True)
    t1.start(); t2.start()
    t1.join(); t2.join()
