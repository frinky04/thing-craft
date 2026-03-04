// Alpha 1.2.6-exact noise primitives.
//
// Ported from Mojang's Java sources:
//   - ImprovedNoise.java   (classic Perlin improved noise)
//   - PerlinNoise.java     (octave stacking of ImprovedNoise)
//   - SimplexNoise.java    (2D simplex noise)
//   - PerlinSimplexNoise.java (octave stacking of SimplexNoise)

// ---------------------------------------------------------------------------
// JavaRandom — java.util.Random LCG
// ---------------------------------------------------------------------------

const MULTIPLIER: i64 = 0x5DEECE66D;
const ADDEND: i64 = 0xB;
const MASK: i64 = (1_i64 << 48) - 1;

#[derive(Debug, Clone)]
pub struct JavaRandom {
    state: i64,
}

impl JavaRandom {
    #[must_use]
    pub fn new(seed: i64) -> Self {
        Self {
            state: (seed ^ MULTIPLIER) & MASK,
        }
    }

    pub fn set_seed(&mut self, seed: i64) {
        self.state = (seed ^ MULTIPLIER) & MASK;
    }

    pub fn next(&mut self, bits: u32) -> i32 {
        self.state = (self.state.wrapping_mul(MULTIPLIER).wrapping_add(ADDEND)) & MASK;
        (self.state >> (48 - bits)) as i32
    }

    pub fn next_int(&mut self, bound: i32) -> i32 {
        assert!(bound > 0);
        // Power-of-two fast path (matches Java exactly)
        if bound & (bound - 1) == 0 {
            return ((bound as i64).wrapping_mul(self.next(31) as i64) >> 31) as i32;
        }
        loop {
            let bits = self.next(31);
            let val = bits % bound;
            if bits.wrapping_sub(val).wrapping_add(bound - 1) >= 0 {
                return val;
            }
        }
    }

    pub fn next_long(&mut self) -> i64 {
        let hi = self.next(32) as i64;
        let lo = self.next(32) as i64;
        (hi << 32).wrapping_add(lo)
    }

    pub fn next_float(&mut self) -> f32 {
        self.next(24) as f32 / (1_i32 << 24) as f32
    }

    pub fn next_double(&mut self) -> f64 {
        let hi = (self.next(26) as i64) << 27;
        let lo = self.next(27) as i64;
        (hi + lo) as f64 / ((1_i64 << 53) as f64)
    }
}

// ---------------------------------------------------------------------------
// ImprovedNoise — Perlin improved noise (ImprovedNoise.java)
// ---------------------------------------------------------------------------

#[derive(Debug, Clone)]
pub struct ImprovedNoise {
    p: [u8; 512],
    xo: f64,
    yo: f64,
    zo: f64,
}

impl ImprovedNoise {
    #[must_use]
    pub fn new(rng: &mut JavaRandom) -> Self {
        let xo = rng.next_double() * 256.0;
        let yo = rng.next_double() * 256.0;
        let zo = rng.next_double() * 256.0;

        let mut p = [0_u8; 512];
        // Initialize identity permutation
        for i in 0..256 {
            p[i] = i as u8;
        }
        // Fisher-Yates shuffle
        for i in 0..256 {
            let j = rng.next_int(256 - i as i32) as usize + i;
            p.swap(i, j);
        }
        // Mirror
        for i in 0..256 {
            p[i + 256] = p[i];
        }

        Self { p, xo, yo, zo }
    }

    #[must_use]
    pub fn noise(&self, x: f64, y: f64, z: f64) -> f64 {
        let x = x + self.xo;
        let y = y + self.yo;
        let z = z + self.zo;

        let xi = x.floor() as i32 & 255;
        let yi = y.floor() as i32 & 255;
        let zi = z.floor() as i32 & 255;

        let xf = x - x.floor();
        let yf = y - y.floor();
        let zf = z - z.floor();

        let u = Self::fade(xf);
        let v = Self::fade(yf);
        let w = Self::fade(zf);

        let a = self.p[xi as usize] as i32 + yi;
        let aa = self.p[a as usize & 511] as i32 + zi;
        let ab = self.p[(a + 1) as usize & 511] as i32 + zi;
        let b = self.p[(xi + 1) as usize & 511] as i32 + yi;
        let ba = self.p[b as usize & 511] as i32 + zi;
        let bb = self.p[(b + 1) as usize & 511] as i32 + zi;

        lerp(
            w,
            lerp(
                v,
                lerp(
                    u,
                    Self::grad(self.p[aa as usize & 511] as i32, xf, yf, zf),
                    Self::grad(self.p[ba as usize & 511] as i32, xf - 1.0, yf, zf),
                ),
                lerp(
                    u,
                    Self::grad(self.p[ab as usize & 511] as i32, xf, yf - 1.0, zf),
                    Self::grad(self.p[bb as usize & 511] as i32, xf - 1.0, yf - 1.0, zf),
                ),
            ),
            lerp(
                v,
                lerp(
                    u,
                    Self::grad(self.p[(aa + 1) as usize & 511] as i32, xf, yf, zf - 1.0),
                    Self::grad(
                        self.p[(ba + 1) as usize & 511] as i32,
                        xf - 1.0,
                        yf,
                        zf - 1.0,
                    ),
                ),
                lerp(
                    u,
                    Self::grad(
                        self.p[(ab + 1) as usize & 511] as i32,
                        xf,
                        yf - 1.0,
                        zf - 1.0,
                    ),
                    Self::grad(
                        self.p[(bb + 1) as usize & 511] as i32,
                        xf - 1.0,
                        yf - 1.0,
                        zf - 1.0,
                    ),
                ),
            ),
        )
    }

    /// Bulk accumulate noise into `values` buffer.
    /// When `size_y == 1`, uses the 2D fast path (y offset = 0, skip y interpolation).
    #[allow(clippy::too_many_arguments)]
    pub fn add(
        &self,
        values: &mut [f64],
        x_origin: f64,
        y_origin: f64,
        z_origin: f64,
        size_x: usize,
        size_y: usize,
        size_z: usize,
        scale_x: f64,
        scale_y: f64,
        scale_z: f64,
        noise_scale: f64,
    ) {
        if size_y == 1 {
            self.add_2d(
                values,
                x_origin,
                z_origin,
                size_x,
                size_z,
                scale_x,
                scale_z,
                noise_scale,
            );
        } else {
            self.add_3d(
                values,
                x_origin,
                y_origin,
                z_origin,
                size_x,
                size_y,
                size_z,
                scale_x,
                scale_y,
                scale_z,
                noise_scale,
            );
        }
    }

    #[allow(clippy::too_many_arguments)]
    fn add_2d(
        &self,
        values: &mut [f64],
        x_origin: f64,
        z_origin: f64,
        size_x: usize,
        size_z: usize,
        scale_x: f64,
        scale_z: f64,
        noise_scale: f64,
    ) {
        let inv = 1.0 / noise_scale;
        let mut idx = 0;
        for ix in 0..size_x {
            let x = (x_origin + ix as f64) * scale_x + self.xo;
            let xi = x.floor() as i32 & 255;
            let xf = x - x.floor();
            let u = Self::fade(xf);

            for iz in 0..size_z {
                let z = (z_origin + iz as f64) * scale_z + self.zo;
                let mut zi = z.floor() as i32;
                let zf = z - zi as f64;
                zi &= 255;
                let w = Self::fade(zf);

                let a = self.p[xi as usize] as i32;
                let aa = self.p[a as usize & 511] as i32 + zi;
                let b = self.p[(xi + 1) as usize & 511] as i32;
                let ba = self.p[b as usize & 511] as i32 + zi;

                let g1 = Self::grad_2d(self.p[aa as usize & 511] as i32, xf, zf);
                let g2 = Self::grad_2d(self.p[ba as usize & 511] as i32, xf - 1.0, zf);
                let l1 = lerp(u, g1, g2);
                let g3 = Self::grad_2d(self.p[(aa + 1) as usize & 511] as i32, xf, zf - 1.0);
                let g4 = Self::grad_2d(self.p[(ba + 1) as usize & 511] as i32, xf - 1.0, zf - 1.0);
                let l2 = lerp(u, g3, g4);
                let result = lerp(w, l1, l2);

                values[idx] += result * inv;
                idx += 1;
            }
        }
    }

    #[allow(clippy::too_many_arguments)]
    fn add_3d(
        &self,
        values: &mut [f64],
        x_origin: f64,
        y_origin: f64,
        z_origin: f64,
        size_x: usize,
        size_y: usize,
        size_z: usize,
        scale_x: f64,
        scale_y: f64,
        scale_z: f64,
        noise_scale: f64,
    ) {
        let inv = 1.0 / noise_scale;
        let mut idx = 0;
        // Java iterates [X][Z][Y] — Y innermost. This must match so that
        // the buffer layout agrees with generateHeightMap's reading order.
        for ix in 0..size_x {
            let x = (x_origin + ix as f64) * scale_x + self.xo;
            let xi = x.floor() as i32 & 255;
            let xf = x - x.floor();
            let u = Self::fade(xf);

            for iz in 0..size_z {
                let z = (z_origin + iz as f64) * scale_z + self.zo;
                let zi = z.floor() as i32 & 255;
                let zf = z - z.floor();
                let w = Self::fade(zf);

                for iy in 0..size_y {
                    let y = (y_origin + iy as f64) * scale_y + self.yo;
                    let yi = y.floor() as i32 & 255;
                    let yf = y - y.floor();
                    let v = Self::fade(yf);

                    let a = self.p[xi as usize] as i32 + yi;
                    let aa = self.p[a as usize & 511] as i32 + zi;
                    let ab = self.p[(a + 1) as usize & 511] as i32 + zi;
                    let b = self.p[(xi + 1) as usize & 511] as i32 + yi;
                    let ba = self.p[b as usize & 511] as i32 + zi;
                    let bb = self.p[(b + 1) as usize & 511] as i32 + zi;

                    let l1 = lerp(
                        u,
                        Self::grad(self.p[aa as usize & 511] as i32, xf, yf, zf),
                        Self::grad(self.p[ba as usize & 511] as i32, xf - 1.0, yf, zf),
                    );
                    let l2 = lerp(
                        u,
                        Self::grad(self.p[ab as usize & 511] as i32, xf, yf - 1.0, zf),
                        Self::grad(self.p[bb as usize & 511] as i32, xf - 1.0, yf - 1.0, zf),
                    );
                    let l3 = lerp(
                        u,
                        Self::grad(self.p[(aa + 1) as usize & 511] as i32, xf, yf, zf - 1.0),
                        Self::grad(
                            self.p[(ba + 1) as usize & 511] as i32,
                            xf - 1.0,
                            yf,
                            zf - 1.0,
                        ),
                    );
                    let l4 = lerp(
                        u,
                        Self::grad(
                            self.p[(ab + 1) as usize & 511] as i32,
                            xf,
                            yf - 1.0,
                            zf - 1.0,
                        ),
                        Self::grad(
                            self.p[(bb + 1) as usize & 511] as i32,
                            xf - 1.0,
                            yf - 1.0,
                            zf - 1.0,
                        ),
                    );

                    let result = lerp(w, lerp(v, l1, l2), lerp(v, l3, l4));
                    values[idx] += result * inv;
                    idx += 1;
                }
            }
        }
    }

    fn fade(t: f64) -> f64 {
        t * t * t * (t * (t * 6.0 - 15.0) + 10.0)
    }

    fn grad(hash: i32, x: f64, y: f64, z: f64) -> f64 {
        let h = hash & 15;
        let u = if h < 8 { x } else { y };
        let v = if h < 4 {
            y
        } else if h == 12 || h == 14 {
            x
        } else {
            z
        };
        let a = if h & 1 == 0 { u } else { -u };
        let b = if h & 2 == 0 { v } else { -v };
        a + b
    }

    fn grad_2d(hash: i32, x: f64, z: f64) -> f64 {
        Self::grad(hash, x, 0.0, z)
    }
}

// ---------------------------------------------------------------------------
// PerlinNoise — octave stacking of ImprovedNoise (PerlinNoise.java)
// ---------------------------------------------------------------------------

#[derive(Debug, Clone)]
pub struct PerlinNoise {
    levels: Vec<ImprovedNoise>,
}

impl PerlinNoise {
    #[must_use]
    pub fn new(rng: &mut JavaRandom, octaves: usize) -> Self {
        let mut levels = Vec::with_capacity(octaves);
        for _ in 0..octaves {
            levels.push(ImprovedNoise::new(rng));
        }
        Self { levels }
    }

    /// Bulk 3D sampling into buffer.
    #[allow(clippy::too_many_arguments)]
    pub fn get_region_3d(
        &self,
        buf: &mut [f64],
        x: f64,
        y: f64,
        z: f64,
        size_x: usize,
        size_y: usize,
        size_z: usize,
        scale_x: f64,
        scale_y: f64,
        scale_z: f64,
    ) {
        // Zero the buffer first
        for v in buf.iter_mut() {
            *v = 0.0;
        }

        let mut amplitude = 1.0;
        for level in &self.levels {
            level.add(
                buf,
                x,
                y,
                z,
                size_x,
                size_y,
                size_z,
                scale_x * amplitude,
                scale_y * amplitude,
                scale_z * amplitude,
                amplitude,
            );
            amplitude /= 2.0;
        }
    }

    /// Bulk 2D sampling (calls 3D with y=10.0, sizeY=1).
    pub fn get_region_2d(
        &self,
        buf: &mut [f64],
        x: f64,
        z: f64,
        size_x: usize,
        size_z: usize,
        scale_x: f64,
        scale_z: f64,
    ) {
        self.get_region_3d(buf, x, 10.0, z, size_x, 1, size_z, scale_x, 1.0, scale_z);
    }
}

// ---------------------------------------------------------------------------
// SimplexNoise — 2D simplex noise (SimplexNoise.java)
// ---------------------------------------------------------------------------

const GRAD3: [[i32; 3]; 12] = [
    [1, 1, 0],
    [-1, 1, 0],
    [1, -1, 0],
    [-1, -1, 0],
    [1, 0, 1],
    [-1, 0, 1],
    [1, 0, -1],
    [-1, 0, -1],
    [0, 1, 1],
    [0, -1, 1],
    [0, 1, -1],
    [0, -1, -1],
];

#[derive(Debug, Clone)]
pub struct SimplexNoise {
    p: [i32; 512],
    xo: f64,
    yo: f64,
    zo: f64,
}

impl SimplexNoise {
    #[must_use]
    pub fn new(rng: &mut JavaRandom) -> Self {
        let xo = rng.next_double() * 256.0;
        let yo = rng.next_double() * 256.0;
        let zo = rng.next_double() * 256.0;

        let mut p = [0_i32; 512];
        for i in 0..256 {
            p[i] = i as i32;
        }
        for i in 0..256 {
            let j = rng.next_int(256 - i as i32) as usize + i;
            p.swap(i, j);
        }
        for i in 0..256 {
            p[i + 256] = p[i];
        }

        Self { p, xo, yo, zo }
    }

    fn dot2(g: [i32; 3], x: f64, y: f64) -> f64 {
        g[0] as f64 * x + g[1] as f64 * y
    }

    #[must_use]
    pub fn value_2d(&self, x: f64, y: f64) -> f64 {
        let f2: f64 = 0.5 * (3.0_f64.sqrt() - 1.0);
        let g2: f64 = (3.0 - 3.0_f64.sqrt()) / 6.0;

        let s = (x + y) * f2;
        let i = (x + s).floor() as i32;
        let j = (y + s).floor() as i32;

        let t = (i + j) as f64 * g2;
        let x0 = x - (i as f64 - t);
        let y0 = y - (j as f64 - t);

        let (i1, j1) = if x0 > y0 { (1, 0) } else { (0, 1) };

        let x1 = x0 - i1 as f64 + g2;
        let y1 = y0 - j1 as f64 + g2;
        let x2 = x0 - 1.0 + 2.0 * g2;
        let y2 = y0 - 1.0 + 2.0 * g2;

        let ii = (i & 255) as usize;
        let jj = (j & 255) as usize;
        let gi0 = self.p[ii + self.p[jj] as usize] as usize % 12;
        let gi1 = self.p[ii + i1 + self.p[jj + j1] as usize] as usize % 12;
        let gi2 = self.p[ii + 1 + self.p[jj + 1] as usize] as usize % 12;

        let mut t0 = 0.5 - x0 * x0 - y0 * y0;
        let n0 = if t0 < 0.0 {
            0.0
        } else {
            t0 *= t0;
            t0 * t0 * Self::dot2(GRAD3[gi0], x0, y0)
        };

        let mut t1 = 0.5 - x1 * x1 - y1 * y1;
        let n1 = if t1 < 0.0 {
            0.0
        } else {
            t1 *= t1;
            t1 * t1 * Self::dot2(GRAD3[gi1], x1, y1)
        };

        let mut t2 = 0.5 - x2 * x2 - y2 * y2;
        let n2 = if t2 < 0.0 {
            0.0
        } else {
            t2 *= t2;
            t2 * t2 * Self::dot2(GRAD3[gi2], x2, y2)
        };

        70.0 * (n0 + n1 + n2)
    }

    /// Bulk accumulate 2D simplex noise into values buffer.
    #[allow(clippy::too_many_arguments)]
    pub fn add(
        &self,
        values: &mut [f64],
        x_origin: f64,
        y_origin: f64,
        size_x: usize,
        size_y: usize,
        scale_x: f64,
        scale_y: f64,
        noise_scale: f64,
    ) {
        let inv = 1.0 / noise_scale;
        let mut idx = 0;
        for iy in 0..size_y {
            let y = (y_origin + iy as f64) * scale_y + self.yo;
            for ix in 0..size_x {
                let x = (x_origin + ix as f64) * scale_x + self.xo;
                values[idx] += self.value_2d(x, y) * inv;
                idx += 1;
            }
        }
    }
}

// ---------------------------------------------------------------------------
// PerlinSimplexNoise — octave stacking of SimplexNoise (PerlinSimplexNoise.java)
// ---------------------------------------------------------------------------

#[derive(Debug, Clone)]
pub struct PerlinSimplexNoise {
    levels: Vec<SimplexNoise>,
}

impl PerlinSimplexNoise {
    #[must_use]
    pub fn new(rng: &mut JavaRandom, octaves: usize) -> Self {
        let mut levels = Vec::with_capacity(octaves);
        for _ in 0..octaves {
            levels.push(SimplexNoise::new(rng));
        }
        Self { levels }
    }

    /// Bulk accumulate octaved simplex noise.
    /// `scale_exponent_x` and `scale_exponent_y` control how frequency/amplitude
    /// change per octave. In Alpha biome code: exponent_x = 0.5, exponent_y varies.
    #[allow(clippy::too_many_arguments)]
    pub fn get_region(
        &self,
        values: &mut [f64],
        x: f64,
        y: f64,
        size_x: usize,
        size_y: usize,
        scale_x: f64,
        scale_y: f64,
        scale_exponent_y: f64,
    ) {
        // Zero the buffer
        for v in values.iter_mut() {
            *v = 0.0;
        }

        let mut d = 1.0_f64;
        let mut e = 1.0_f64;
        for level in &self.levels {
            level.add(
                values,
                x,
                y,
                size_x,
                size_y,
                scale_x * e / 1.5,
                scale_y * e / 1.5,
                0.55 / d,
            );
            d *= scale_exponent_y;
            e *= 0.5; // scaleExponentX is always 0.5 in Alpha
        }
    }
}

// ---------------------------------------------------------------------------
// Utility
// ---------------------------------------------------------------------------

fn lerp(t: f64, a: f64, b: f64) -> f64 {
    a + t * (b - a)
}
