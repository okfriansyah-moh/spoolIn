#include <vips/vips.h>
#include <stdio.h>

/* Fixed internal helper: no user-selected options and no image bytes on stdout. */
int main(int argc, char **argv) {
    if (argc != 3 || VIPS_INIT(argv[0])) return 1;
    VipsImage *image = vips_image_new_from_file(argv[1], "access", VIPS_ACCESS_SEQUENTIAL,
                                               "fail_on", VIPS_FAIL_ON_WARNING, NULL);
    if (!image) return 2;
    int width = vips_image_get_width(image), height = vips_image_get_height(image);
    int bands = vips_image_get_bands(image);
    int pages = 1;
    if (vips_image_get_typeof(image, "n-pages")) vips_image_get_int(image, "n-pages", &pages);
    if (width < 1 || width > 8192 || height < 1 || height > 8192 || bands < 1 || bands > 4 ||
        pages != 1 || (long long)width * height > 40000000 ||
        (long long)width * height * bands * vips_format_sizeof(image->BandFmt) > 160000000) return 3;
    FILE *output = fopen(argv[2], "w");
    if (!output) return 4;
    int result = fprintf(output, "%d %d %d\n", width, height, bands) < 0;
    result |= fclose(output) != 0;
    g_object_unref(image);
    vips_shutdown();
    return result;
}
