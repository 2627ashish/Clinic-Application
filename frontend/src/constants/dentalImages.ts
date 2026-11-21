const imageUrl = (photoId: string, width: number) =>
  `https://images.unsplash.com/${photoId}?auto=format&fit=crop&w=${width}&q=80`;

const serviceImageUrl = (photoId: string) =>
  `https://images.unsplash.com/${photoId}?auto=format&fit=crop&w=600&h=400&q=80`;

export const DENTAL_IMAGES = {
  doctorHero: imageUrl('photo-1629909613654-28e377c37b09', 1000),
  doctorHeroFallback: imageUrl('photo-1612349317150-e413f6a5b16d', 1000),
  clinicAmbience: imageUrl('photo-1588776814546-1ffcf47267a5', 1200),
  happySmile: imageUrl('photo-1598256989800-fe5f95da9787', 800),
  services: {
    rct: serviceImageUrl('photo-1606811841689-23dfddce3e95'),
    implants: serviceImageUrl('photo-1588776814546-1ffcf47267a5'),
    scaling: serviceImageUrl('photo-1609840114035-3c981b782dfe'),
    fillings: serviceImageUrl('photo-1579684385127-1ef15d508118'),
    braces: serviceImageUrl('photo-1598256989800-fe5f95da9787'),
    xray: serviceImageUrl('photo-1516549655169-df83a0774514'),
    dentures: imageUrl('photo-1571772996211-2f02c9727629', 800),
    extraction: serviceImageUrl('photo-1629909615184-74f495363b67'),
  },
} as const;
