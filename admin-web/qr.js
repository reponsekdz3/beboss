// Pure-JS ISO/IEC 18004 QR Code Encoder (Versions 1..14, Byte mode, Error Correction M)
(function (global) {
  const GF_EXP = new Uint8Array(512);
  const GF_LOG = new Uint8Array(256);
  let x = 1;
  for (let i = 0; i < 255; i++) {
    GF_EXP[i] = x;
    GF_LOG[x] = i;
    x <<= 1;
    if (x & 256) x ^= 285;
  }
  for (let i = 255; i < 512; i++) GF_EXP[i] = GF_EXP[i - 255];

  const gfMul = (a, b) => (a && b ? GF_EXP[GF_LOG[a] + GF_LOG[b]] : 0);

  // [totalDataCodewords, ecPerBlock, g1Blocks, g1Data, g2Blocks, g2Data]
  const VER_M = {
    1: [16, 10, 1, 16, 0, 0],
    2: [28, 16, 1, 28, 0, 0],
    3: [44, 26, 1, 44, 0, 0],
    4: [64, 18, 2, 32, 0, 0],
    5: [86, 24, 2, 43, 0, 0],
    6: [108, 16, 4, 27, 0, 0],
    7: [124, 18, 4, 31, 0, 0],
    8: [154, 22, 2, 38, 2, 39],
    9: [182, 22, 3, 36, 2, 37],
    10: [216, 26, 4, 43, 1, 44],
    11: [254, 30, 1, 50, 4, 51],
    12: [290, 22, 6, 36, 2, 37],
    13: [334, 22, 8, 37, 1, 38],
    14: [365, 24, 4, 40, 5, 41]
  };

  const ALIGN = {
    1: [],
    2: [6, 18],
    3: [6, 22],
    4: [6, 26],
    5: [6, 30],
    6: [6, 34],
    7: [6, 22, 38],
    8: [6, 24, 42],
    9: [6, 26, 46],
    10: [6, 28, 50],
    11: [6, 30, 54],
    12: [6, 32, 58],
    13: [6, 34, 62],
    14: [6, 26, 46, 66]
  };

  function rsGenerator(deg) {
    let g = [1];
    for (let i = 0; i < deg; i++) {
      const next = new Array(g.length + 1).fill(0);
      const r = GF_EXP[i];
      for (let j = 0; j < g.length; j++) {
        next[j] ^= g[j];
        next[j + 1] ^= gfMul(g[j], r);
      }
      g = next;
    }
    return g;
  }

  function rsRemainder(data, ecLen) {
    const g = rsGenerator(ecLen);
    const res = data.concat(new Array(ecLen).fill(0));
    for (let i = 0; i < data.length; i++) {
      const c = res[i];
      if (c !== 0) {
        for (let j = 0; j < g.length; j++) res[i + j] ^= gfMul(g[j], c);
      }
    }
    return res.slice(data.length);
  }

  function encodeQR(text) {
    const bytes = Array.from(new TextEncoder().encode(String(text || 'https://beboss.app')));
    let ver = 1;
    while (ver <= 14) {
      const lenBits = ver >= 10 ? 16 : 8;
      const capBytes = Math.floor((VER_M[ver][0] * 8 - 4 - lenBits) / 8);
      if (bytes.length <= capBytes) break;
      ver++;
    }
    if (ver > 14) ver = 14;
    const [totalData, ecLen, b1, d1, b2, d2] = VER_M[ver];
    const lenBits = ver >= 10 ? 16 : 8;
    const maxDataBytes = Math.floor((totalData * 8 - 4 - lenBits) / 8);
    const payloadBytes = bytes.slice(0, maxDataBytes);

    const bits = [];
    const pushBits = (val, len) => {
      for (let i = len - 1; i >= 0; i--) bits.push((val >> i) & 1);
    };
    pushBits(0b0100, 4);
    pushBits(payloadBytes.length, lenBits);
    for (let i = 0; i < payloadBytes.length; i++) pushBits(payloadBytes[i], 8);
    const maxBits = totalData * 8;
    const term = Math.min(4, maxBits - bits.length);
    for (let i = 0; i < term; i++) bits.push(0);
    while (bits.length % 8 !== 0) bits.push(0);

    const codewords = [];
    for (let i = 0; i < bits.length; i += 8) {
      let b = 0;
      for (let k = 0; k < 8; k++) b = (b << 1) | bits[i + k];
      codewords.push(b);
    }
    let pad = 0;
    while (codewords.length < totalData) codewords.push(pad++ % 2 === 0 ? 0xec : 0x11);

    const blocks = [];
    const ecBlocks = [];
    let off = 0;
    for (let i = 0; i < b1; i++) {
      const bl = codewords.slice(off, off + d1);
      off += d1;
      blocks.push(bl);
      ecBlocks.push(rsRemainder(bl, ecLen));
    }
    for (let i = 0; i < b2; i++) {
      const bl = codewords.slice(off, off + d2);
      off += d2;
      blocks.push(bl);
      ecBlocks.push(rsRemainder(bl, ecLen));
    }

    const interleaved = [];
    const maxD = Math.max(d1, d2);
    for (let i = 0; i < maxD; i++) for (const b of blocks) if (i < b.length) interleaved.push(b[i]);
    for (let i = 0; i < ecLen; i++) for (const e of ecBlocks) interleaved.push(e[i]);

    const size = ver * 4 + 17;
    const mod = Array.from({ length: size }, () => new Array(size).fill(-1));
    const func = Array.from({ length: size }, () => new Array(size).fill(false));
    const setF = (r, c, v) => {
      if (r >= 0 && r < size && c >= 0 && c < size) {
        mod[r][c] = v ? 1 : 0;
        func[r][c] = true;
      }
    };

    const drawFinder = (r0, c0) => {
      for (let r = -1; r <= 7; r++) {
        for (let c = -1; c <= 7; c++) {
          const rr = r0 + r;
          const cc = c0 + c;
          if (rr < 0 || rr >= size || cc < 0 || cc >= size) continue;
          const on =
            r >= 0 &&
            r <= 6 &&
            c >= 0 &&
            c <= 6 &&
            (r === 0 || r === 6 || c === 0 || c === 6 || (r >= 2 && r <= 4 && c >= 2 && c <= 4));
          setF(rr, cc, on);
        }
      }
    };
    drawFinder(0, 0);
    drawFinder(0, size - 7);
    drawFinder(size - 7, 0);

    const coords = ALIGN[ver];
    for (const ar of coords) {
      for (const ac of coords) {
        if (func[ar][ac]) continue;
        for (let r = -2; r <= 2; r++) {
          for (let c = -2; c <= 2; c++) {
            setF(ar + r, ac + c, Math.max(Math.abs(r), Math.abs(c)) !== 1);
          }
        }
      }
    }

    for (let i = 8; i < size - 8; i++) {
      setF(6, i, i % 2 === 0);
      setF(i, 6, i % 2 === 0);
    }
    setF(size - 8, 8, true);

    for (let i = 0; i < 9; i++) {
      if (!func[8][i]) setF(8, i, false);
      if (!func[i][8]) setF(i, 8, false);
    }
    for (let i = 0; i < 8; i++) {
      if (!func[8][size - 1 - i]) setF(8, size - 1 - i, false);
      if (i < 7 && !func[size - 1 - i][8]) setF(size - 1 - i, 8, false);
    }

    if (ver >= 7) {
      let rem = ver << 12;
      for (let i = 17; i >= 12; i--) if ((rem >>> i) & 1) rem ^= 0x1f25 << (i - 12);
      const vb = (ver << 12) | rem;
      for (let i = 0; i < 18; i++) {
        const bit = ((vb >> i) & 1) === 1;
        setF(Math.floor(i / 3), size - 11 + (i % 3), bit);
        setF(size - 11 + (i % 3), Math.floor(i / 3), bit);
      }
    }

    const dataBits = [];
    for (const cw of interleaved) for (let b = 7; b >= 0; b--) dataBits.push((cw >> b) & 1);
    let bitIdx = 0;
    for (let right = size - 1; right >= 1; right -= 2) {
      if (right === 6) right = 5;
      for (let vert = 0; vert < size; vert++) {
        for (let j = 0; j < 2; j++) {
          const c = right - j;
          const up = ((right + 1) & 2) === 0;
          const r = up ? size - 1 - vert : vert;
          if (!func[r][c]) {
            mod[r][c] = bitIdx < dataBits.length ? dataBits[bitIdx++] : 0;
          }
        }
      }
    }

    const masks = [
      (r, c) => (r + c) % 2 === 0,
      (r, c) => r % 2 === 0,
      (r, c) => c % 3 === 0,
      (r, c) => (r + c) % 3 === 0,
      (r, c) => (Math.floor(r / 2) + Math.floor(c / 3)) % 2 === 0,
      (r, c) => ((r * c) % 2) + ((r * c) % 3) === 0,
      (r, c) => (((r * c) % 2) + ((r * c) % 3)) % 2 === 0,
      (r, c) => (((r + c) % 2) + ((r * c) % 3)) % 2 === 0
    ];

    let bestScore = Infinity;
    let bestGrid = null;
    for (let m = 0; m < 8; m++) {
      const g = mod.map((row, r) => row.map((val, c) => (!func[r][c] && masks[m](r, c) ? val ^ 1 : val)));
      const fmtData = (0b00 << 3) | m;
      let rem = fmtData << 10;
      for (let i = 14; i >= 10; i--) if ((rem >>> i) & 1) rem ^= 0x537 << (i - 10);
      const fmtBits = ((fmtData << 10) | rem) ^ 0x5412;

      for (let i = 0; i <= 5; i++) g[8][i] = (fmtBits >> (14 - i)) & 1;
      g[8][7] = (fmtBits >> 8) & 1;
      g[8][8] = (fmtBits >> 7) & 1;
      g[7][8] = (fmtBits >> 6) & 1;
      for (let i = 0; i <= 5; i++) g[5 - i][8] = (fmtBits >> (5 - i)) & 1;

      for (let i = 0; i < 7; i++) g[size - 1 - i][8] = (fmtBits >> (14 - i)) & 1;
      g[size - 8][8] = 1;
      for (let i = 0; i < 8; i++) g[8][size - 8 + i] = (fmtBits >> (7 - i)) & 1;

      let score = 0;
      for (let r = 0; r < size; r++) {
        let run = 1;
        for (let c = 1; c < size; c++) {
          if (g[r][c] === g[r][c - 1]) run++;
          else {
            if (run >= 5) score += run - 2;
            run = 1;
          }
        }
        if (run >= 5) score += run - 2;
      }
      for (let c = 0; c < size; c++) {
        let run = 1;
        for (let r = 1; r < size; r++) {
          if (g[r][c] === g[r - 1][c]) run++;
          else {
            if (run >= 5) score += run - 2;
            run = 1;
          }
        }
        if (run >= 5) score += run - 2;
      }
      for (let r = 0; r < size - 1; r++) {
        for (let c = 0; c < size - 1; c++) {
          const s = g[r][c] + g[r][c + 1] + g[r + 1][c] + g[r + 1][c + 1];
          if (s === 0 || s === 4) score += 3;
        }
      }
      if (score < bestScore) {
        bestScore = score;
        bestGrid = g;
      }
    }
    return { size, ver, grid: bestGrid };
  }

  function downloadQrPng(text, filename = 'beboss-download-qr.png', subtitle = 'Scan to Download BeBoss POS APK') {
    const qr = encodeQR(text);
    const scale = 10;
    const quiet = 4;
    const qrPx = (qr.size + quiet * 2) * scale;
    const footerH = 66;
    const canvas = document.createElement('canvas');
    canvas.width = qrPx;
    canvas.height = qrPx + footerH;
    const ctx = canvas.getContext('2d');
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    ctx.fillStyle = '#070b14';
    for (let r = 0; r < qr.size; r++) {
      for (let c = 0; c < qr.size; c++) {
        if (qr.grid[r][c] === 1) {
          ctx.fillRect((c + quiet) * scale, (r + quiet) * scale, scale, scale);
        }
      }
    }

    ctx.fillStyle = '#f97316';
    ctx.fillRect(0, qrPx, canvas.width, 4);
    ctx.fillStyle = '#0f172a';
    ctx.font = 'bold 18px Inter, system-ui, sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('BEBOSS ENTERPRISE POS', canvas.width / 2, qrPx + 28);
    ctx.fillStyle = '#475569';
    ctx.font = '600 13px Inter, system-ui, sans-serif';
    ctx.fillText(subtitle, canvas.width / 2, qrPx + 50);

    const a = document.createElement('a');
    a.href = canvas.toDataURL('image/png');
    a.download = filename;
    a.click();
  }

  global.BeBossQR = { encodeQR, downloadQrPng };
})(window);
