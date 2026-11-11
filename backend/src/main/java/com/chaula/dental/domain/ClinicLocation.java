package com.chaula.dental.domain;

public enum ClinicLocation {
    SEC_44(
            "Clinic 1 · Sec-44, Chhalera",
            "Gali No. 3, Chhalera, Sec-44, Noida",
            "Mon-Sat: 10:00 AM–1:00 PM | 5:30 PM–9:00 PM",
            "https://www.google.com/maps/place/28%C2%B033'28.3%22N+77%C2%B020'38.1%22E/@28.5578734,77.3439285,17z/data=!3m1!4b1!4m4!3m3!8m2!3d28.5578734!4d77.3439285?entry=ttu&g_ep=EgoyMDI2MTEwNC4wIKXMDSoASAFQAw%3D%3D",
            28.5578734,
            77.3439285
    ),
    SEC_82(
            "Clinic 2 · Sec-82, Madhuban",
            "78A MIG, Madhuban Apartments, Sec-82, Noida",
            "Mon-Sat: 8:30 AM–10:00 AM | 2:00 PM–5:00 PM",
            "https://www.google.com/maps/place/78,+Swarnim+Vihar,+Sector+82,+Noida,+Uttar+Pradesh+201304/@28.5296124,77.3844855,17z/data=!3m1!4b1!4m6!3m5!1s0x390ce8a377ea0455:0xdf316b5ab3199f24!8m2!3d28.5296124!4d77.3870604!16s%2Fg%2F11wb219d5k?entry=ttu&g_ep=EgoyMDI2MTEwNC4wIKXMDSoASAFQAw%3D%3D",
            28.5296124,
            77.3870604
    );

    private final String label;
    private final String address;
    private final String hours;
    private final String directions;
    private final double latitude;
    private final double longitude;

    ClinicLocation(
            String label, String address, String hours, String directions, double latitude, double longitude
    ) {
        this.label = label;
        this.address = address;
        this.hours = hours;
        this.directions = directions;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String label() {
        return label;
    }

    public String address() {
        return address;
    }

    public String hours() {
        return hours;
    }

    public String directions() {
        return directions;
    }

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }

    public static ClinicLocation defaultLocation(ClinicLocation location) {
        return location == null ? SEC_44 : location;
    }
}
