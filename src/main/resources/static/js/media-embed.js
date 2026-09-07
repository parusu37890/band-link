// A profile's one link used to be assumed to be YouTube: anything else fell through to a bare
// "open the video" link. Bands post to more than one place, so each service that offers an embed
// gets recognised here and the rest keep the link, which is still the honest fallback.
//
// Every embed URL is built from parsed pieces, never from the string the person typed, and each
// id is checked against a pattern before it goes anywhere near a src. The host list is exact —
// a suffix match would let evil-youtube.com through.
//
// Shapes differ far more than the old fixed 16/9 allowed: a SoundCloud player is a 166px strip,
// a TikTok is taller than it is wide, and a Spotify album is twice the height of a single track.
// Each provider says how tall it wants to be and .media-frame does as it is told.

const host = url => url.hostname.toLowerCase().replace(/^www\./, '');
const YT_ID = /^[\w-]{11}$/;
const DIGITS = /^\d+$/;
const SPOTIFY_ID = /^[A-Za-z0-9]{22}$/;

/** open.spotify.com/track/<id>, and the /intl-ja/ prefix its share links carry in Japan. */
function spotify(url) {
  const parts = url.pathname.split('/').filter(Boolean);
  if (parts[0] && parts[0].startsWith('intl-')) parts.shift();
  const [kind, id] = parts;
  const tall = ['album', 'playlist', 'artist', 'show'];
  if (!['track', 'episode', ...tall].includes(kind) || !SPOTIFY_ID.test(id || '')) return null;
  // A single track draws a compact bar and leaves the rest of Spotify's documented 152px empty.
  return { src: `https://open.spotify.com/embed/${kind}/${id}`, height: tall.includes(kind) ? 352 : 80 };
}

/** music.apple.com/<storefront>/<kind>/<slug>/<id> embeds at the same path on embed.music.apple.com. */
function appleMusic(url) {
  const parts = url.pathname.split('/').filter(Boolean);
  const kind = parts[1];
  if (!['album', 'playlist', 'song'].includes(kind)) return null;
  if (!/^[a-z]{2}$/.test(parts[0] || '')) return null;
  if (!parts.every(p => /^[\w.%-]+$/.test(p))) return null;
  // A song shared from an album arrives as .../album/<slug>/<id>?i=<songId>, which the embed keeps.
  const track = url.searchParams.get('i');
  const query = track && DIGITS.test(track) ? `?i=${track}` : '';
  const height = kind === 'song' || track ? 175 : 450;
  return { src: `https://embed.music.apple.com/${parts.join('/')}${query}`, height };
}

const providers = [
  {
    name: 'YouTube',
    hosts: ['youtube.com', 'm.youtube.com', 'youtu.be', 'youtube-nocookie.com'],
    build(url) {
      const id = host(url) === 'youtu.be'
        ? url.pathname.slice(1)
        : url.searchParams.get('v') || url.pathname.split('/').pop();
      return YT_ID.test(id || '') ? { src: `https://www.youtube-nocookie.com/embed/${id}`, ratio: '16 / 9' } : null;
    },
  },
  {
    name: 'TikTok',
    hosts: ['tiktok.com', 'm.tiktok.com', 'vm.tiktok.com'],
    build(url) {
      // Only the full /video/<id> form carries an id; vm.tiktok.com short links resolve server-side,
      // so those stay a link rather than a guess.
      const id = url.pathname.split('/').filter(Boolean).pop();
      return DIGITS.test(id || '') ? { src: `https://www.tiktok.com/embed/v2/${id}`, height: 750, width: 325 } : null;
    },
  },
  {
    name: 'SoundCloud',
    hosts: ['soundcloud.com', 'm.soundcloud.com', 'on.soundcloud.com'],
    build(url) {
      // The player takes the track's own URL, so it is rebuilt from the parsed host and path
      // rather than passed through, and only ever points back at soundcloud.com.
      const path = url.pathname.split('/').filter(Boolean);
      if (!path.length || !path.every(p => /^[\w.-]+$/.test(p))) return null;
      const track = encodeURIComponent(`https://soundcloud.com/${path.join('/')}`);
      return { src: `https://w.soundcloud.com/player/?url=${track}&color=%2355794b&show_comments=false`, height: 166 };
    },
  },
  { name: 'Spotify', hosts: ['open.spotify.com', 'spotify.com'], build: spotify },
  { name: 'Apple Music', hosts: ['music.apple.com', 'embed.music.apple.com'], build: appleMusic },
];

/** The link itself, once it is known to be http(s). Empty for anything else. */
export function mediaHref(value) {
  try {
    const url = new URL(value);
    return ['http:', 'https:'].includes(url.protocol) ? url.href : '';
  } catch {
    return '';
  }
}

/** Which service a link belongs to, for naming the fallback. Null when it is not one we know. */
export function mediaProvider(value) {
  try {
    const url = new URL(value);
    if (!['http:', 'https:'].includes(url.protocol)) return null;
    return providers.find(p => p.hosts.includes(host(url)))?.name || null;
  } catch {
    return null;
  }
}

/**
 * An embed for a link we recognise: {name, src, ratio|height, width}. Null when the service is
 * unknown, or known but the link does not carry the id its player needs — a profile page, say.
 */
export function mediaEmbed(value) {
  try {
    const url = new URL(value);
    if (!['http:', 'https:'].includes(url.protocol)) return null;
    const provider = providers.find(p => p.hosts.includes(host(url)));
    if (!provider) return null;
    const embed = provider.build(url);
    return embed ? { name: provider.name, ...embed } : null;
  } catch {
    return null;
  }
}

export const mediaServices = providers.map(p => p.name);
