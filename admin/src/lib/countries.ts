type CountryDefinition = readonly [name: string, code: string];

const COUNTRY_DEFINITIONS: readonly CountryDefinition[] = [
  ['Albania', 'AL'],
  ['Algeria', 'DZ'],
  ['Argentina', 'AR'],
  ['Australia', 'AU'],
  ['Austria', 'AT'],
  ['Azerbaijan', 'AZ'],
  ['Bahrain', 'BH'],
  ['Bangladesh', 'BD'],
  ['Belgium', 'BE'],
  ['Bosnia and Herzegovina', 'BA'],
  ['Brazil', 'BR'],
  ['Brunei', 'BN'],
  ['Cambodia', 'KH'],
  ['Cameroon', 'CM'],
  ['Canada', 'CA'],
  ['Chad', 'TD'],
  ['China', 'CN'],
  ["Côte d'Ivoire", 'CI'],
  ['Cyprus', 'CY'],
  ['Czechia', 'CZ'],
  ['Denmark', 'DK'],
  ['Egypt', 'EG'],
  ['Ethiopia', 'ET'],
  ['Finland', 'FI'],
  ['France', 'FR'],
  ['Germany', 'DE'],
  ['Ghana', 'GH'],
  ['Greece', 'GR'],
  ['India', 'IN'],
  ['Indonesia', 'ID'],
  ['Iran', 'IR'],
  ['Iraq', 'IQ'],
  ['Ireland', 'IE'],
  ['Italy', 'IT'],
  ['Japan', 'JP'],
  ['Jordan', 'JO'],
  ['Kazakhstan', 'KZ'],
  ['Kosovo', 'XK'],
  ['Kuwait', 'KW'],
  ['Laos', 'LA'],
  ['Lebanon', 'LB'],
  ['Libya', 'LY'],
  ['Malaysia', 'MY'],
  ['Mali', 'ML'],
  ['Mauritius', 'MU'],
  ['Mexico', 'MX'],
  ['Morocco', 'MA'],
  ['Mozambique', 'MZ'],
  ['Myanmar', 'MM'],
  ['Netherlands', 'NL'],
  ['New Zealand', 'NZ'],
  ['Niger', 'NE'],
  ['Nigeria', 'NG'],
  ['Norway', 'NO'],
  ['Oman', 'OM'],
  ['Pakistan', 'PK'],
  ['Palestine', 'PS'],
  ['Philippines', 'PH'],
  ['Poland', 'PL'],
  ['Portugal', 'PT'],
  ['Qatar', 'QA'],
  ['Romania', 'RO'],
  ['Russia', 'RU'],
  ['Saudi Arabia', 'SA'],
  ['Senegal', 'SN'],
  ['Singapore', 'SG'],
  ['Somalia', 'SO'],
  ['South Africa', 'ZA'],
  ['South Korea', 'KR'],
  ['Spain', 'ES'],
  ['Sudan', 'SD'],
  ['Sweden', 'SE'],
  ['Switzerland', 'CH'],
  ['Tajikistan', 'TJ'],
  ['Thailand', 'TH'],
  ['Tunisia', 'TN'],
  ['Turkey', 'TR'],
  ['United Arab Emirates', 'AE'],
  ['United Kingdom', 'GB'],
  ['United States', 'US'],
  ['Vietnam', 'VN'],
  ['Yemen', 'YE'],
];

const COUNTRY_CODES = Object.fromEntries(
  COUNTRY_DEFINITIONS.map(([name, code]) => [name, code]),
) as Record<string, string>;

const COUNTRY_ALIASES: Record<string, string> = {
  'albania': 'Albania',
  'algeria': 'Algeria',
  'argentina': 'Argentina',
  'australia': 'Australia',
  'austria': 'Austria',
  'azerbaijan': 'Azerbaijan',
  'bahrain': 'Bahrain',
  'bangladesh': 'Bangladesh',
  'belgium': 'Belgium',
  'bosnia': 'Bosnia and Herzegovina',
  'bosnia and herzegovina': 'Bosnia and Herzegovina',
  'brazil': 'Brazil',
  'brunei': 'Brunei',
  'cambodia': 'Cambodia',
  'cameroon': 'Cameroon',
  'canada': 'Canada',
  'chad': 'Chad',
  'china': 'China',
  'côte d’ivoire': "Côte d'Ivoire",
  'côte divoire': "Côte d'Ivoire",
  'cote d ivoire': "Côte d'Ivoire",
  'cote divoire': "Côte d'Ivoire",
  'cyprus': 'Cyprus',
  'czech republic': 'Czechia',
  'czechia': 'Czechia',
  'denmark': 'Denmark',
  'egypt': 'Egypt',
  'ethiopia': 'Ethiopia',
  'finland': 'Finland',
  'france': 'France',
  'germany': 'Germany',
  'ghana': 'Ghana',
  'great britain': 'United Kingdom',
  'greece': 'Greece',
  'holland': 'Netherlands',
  'india': 'India',
  'indonesia': 'Indonesia',
  'iran': 'Iran',
  'iraq': 'Iraq',
  'iraq (kurdistan)': 'Iraq',
  'ireland': 'Ireland',
  'italy': 'Italy',
  'ivory coast': "Côte d'Ivoire",
  'japan': 'Japan',
  'jordan': 'Jordan',
  'korea': 'South Korea',
  'korea, south': 'South Korea',
  'kazakhstan': 'Kazakhstan',
  'kosovo': 'Kosovo',
  'kuwait': 'Kuwait',
  'laos': 'Laos',
  'lebanon': 'Lebanon',
  'libya': 'Libya',
  'malaysia': 'Malaysia',
  'mali': 'Mali',
  'mauritius': 'Mauritius',
  'mexico': 'Mexico',
  'morocco': 'Morocco',
  'mozambique': 'Mozambique',
  'myanmar': 'Myanmar',
  'netherlands': 'Netherlands',
  'new zealand': 'New Zealand',
  'niger': 'Niger',
  'nigeria': 'Nigeria',
  'norway': 'Norway',
  'oman': 'Oman',
  'pakistan': 'Pakistan',
  'palestine': 'Palestine',
  'philippines': 'Philippines',
  'poland': 'Poland',
  'portugal': 'Portugal',
  'qatar': 'Qatar',
  'republic of korea': 'South Korea',
  'romania': 'Romania',
  'russia': 'Russia',
  'russian federation': 'Russia',
  'saudi': 'Saudi Arabia',
  'saudi arabia': 'Saudi Arabia',
  'senegal': 'Senegal',
  'singapore': 'Singapore',
  'somalia': 'Somalia',
  'south africa': 'South Africa',
  'south korea': 'South Korea',
  'spain': 'Spain',
  'sudan': 'Sudan',
  'sweden': 'Sweden',
  'switzerland': 'Switzerland',
  'tajikistan': 'Tajikistan',
  'thailand': 'Thailand',
  'tunisia': 'Tunisia',
  'turkey': 'Turkey',
  'turkiye': 'Turkey',
  'uae': 'United Arab Emirates',
  'uk': 'United Kingdom',
  'united arab emirates': 'United Arab Emirates',
  'united kingdom': 'United Kingdom',
  'united states': 'United States',
  'united states of america': 'United States',
  'us': 'United States',
  'usa': 'United States',
  'u.s.a': 'United States',
  'u.s.a.': 'United States',
  'u.k': 'United Kingdom',
  'u.k.': 'United Kingdom',
  'vietnam': 'Vietnam',
  'yemen': 'Yemen',
  'burma': 'Myanmar',
  'مصر': 'Egypt',
  'السعودية': 'Saudi Arabia',
  'المملكة العربية السعودية': 'Saudi Arabia',
  'الكويت': 'Kuwait',
  'المغرب': 'Morocco',
  'السودان': 'Sudan',
  'ليبيا': 'Libya',
  'الأردن': 'Jordan',
  'الاردن': 'Jordan',
  'فلسطين': 'Palestine',
  'العراق': 'Iraq',
  'الإمارات': 'United Arab Emirates',
  'الامارات': 'United Arab Emirates',
  'قطر': 'Qatar',
  'عمان': 'Oman',
  'عُمان': 'Oman',
  'البحرين': 'Bahrain',
  'اليمن': 'Yemen',
  'تركيا': 'Turkey',
  'إيران': 'Iran',
  'باكستان': 'Pakistan',
  'إندونيسيا': 'Indonesia',
  'اندونيسيا': 'Indonesia',
  'ماليزيا': 'Malaysia',
  'الهند': 'India',
  'بنغلاديش': 'Bangladesh',
  'الفلبين': 'Philippines',
  'سنغافورة': 'Singapore',
  'نيجيريا': 'Nigeria',
  'السنغال': 'Senegal',
  'مالي': 'Mali',
  'كوت ديفوار': "Côte d'Ivoire",
  'غانا': 'Ghana',
  'جنوب أفريقيا': 'South Africa',
  'الصومال': 'Somalia',
  'إريتريا': 'Eritrea',
  'روسيا': 'Russia',
  'الولايات المتحدة': 'United States',
  'فرنسا': 'France',
  'المملكة المتحدة': 'United Kingdom',
  'ألمانيا': 'Germany',
  'إسبانيا': 'Spain',
  'أستراليا': 'Australia',
  'تايلاند': 'Thailand',
  'الصين': 'China',
  'اليابان': 'Japan',
  'الهولندا': 'Netherlands',
};

const EMOJI_PATTERN = /[\p{Regional_Indicator}\p{Extended_Pictographic}\p{Emoji_Presentation}\u200D\uFE0F]/gu;

function lookupKey(value: string): string {
  return value.normalize('NFKC').toLowerCase().replace(/[.·]/g, '').replace(/\s+/g, ' ').trim();
}

function stripEmoji(value: string): string {
  return value.replace(EMOJI_PATTERN, '').replace(/\s+/g, ' ').trim();
}

function canonicalName(value: string): string | undefined {
  return COUNTRY_ALIASES[lookupKey(value)];
}

function flagFromCode(code: string): string {
  return String.fromCodePoint(
    ...code.toUpperCase().split('').map((letter) => 0x1f1e6 + letter.charCodeAt(0) - 65),
  );
}

function countryCode(value: string): string | undefined {
  return COUNTRY_CODES[value];
}

/** Removes flags and other emoji, trims whitespace, and resolves known country aliases. */
export function normalizeCountryName(value: string): string {
  const cleaned = stripEmoji(value);
  const directMatch = canonicalName(cleaned);
  if (directMatch) return directMatch;

  const parts = cleaned.split(/\s*\/\s*/);
  if (parts.length > 1) {
    return parts.map((part) => canonicalName(part) ?? part).join(' / ');
  }

  return cleaned;
}

/** Returns the computed ISO-3166 alpha-2 flag for a country, or an empty string when unknown. */
export function countryFlag(country: string): string {
  const name = normalizeCountryName(country);
  const code = countryCode(name);
  return code ? flagFromCode(code) : '';
}

/** Returns a country with its computed flag, preserving unknown country names unchanged. */
export function countryWithFlag(country: string): string {
  const name = normalizeCountryName(country);
  const code = countryCode(name);
  if (code) return `${name} ${flagFromCode(code)}`;

  const parts = name.split(/\s*\/\s*/);
  if (parts.length > 1 && parts.every((part) => countryCode(part))) {
    return parts.map((part) => `${part} ${flagFromCode(countryCode(part) as string)}`).join(' / ');
  }

  return name;
}

/** Returns the supported canonical countries and their computed flags, sorted by name. */
export function countryOptions(): Array<{ name: string; flag: string }> {
  return COUNTRY_DEFINITIONS
    .map(([name, code]) => ({ name, flag: flagFromCode(code) }))
    .sort((left, right) => left.name.localeCompare(right.name));
}

/** Splits a stored country value into its name and computed flag. */
export function splitCountryWithFlag(value: string): { name: string; flag: string } {
  const name = normalizeCountryName(value);
  return { name, flag: countryFlag(name) };
}
