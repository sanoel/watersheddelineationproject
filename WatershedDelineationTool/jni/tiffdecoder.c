/* libtiffdecoder A tiff decoder run on android system. Copyright (C) 2009 figofuture
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public 
 * License as published by the Free Software Foundation; either version 2.1 of the License, or (at your option) any later 
 * version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free 
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 * 
 * */

#include <jni.h>
#include <stdio.h>
#include <tiffio.h>
#include <android/log.h>
#include <stdlib.h>

#define	N(a)	(sizeof (a) / sizeof (a[0]))

/* tags 33550 is a private tag registered to SoftDesk, Inc */
#define TIFFTAG_GEOPIXELSCALE       33550
/* tags 33920-33921 are private tags registered to Intergraph, Inc */
#define TIFFTAG_INTERGRAPH_MATRIX    33920   /* $use TIFFTAG_GEOTRANSMATRIX ! */
#define TIFFTAG_GEOTIEPOINTS         33922
/* tags 34263-34264 are private tags registered to NASA-JPL Carto Group */
#ifdef JPL_TAG_SUPPORT
#define TIFFTAG_JPL_CARTO_IFD        34263    /* $use GeoProjectionInfo ! */
#endif
#define TIFFTAG_GEOTRANSMATRIX       34264    /* New Matrix Tag replaces 33920 */
/* tags 34735-3438 are private tags registered to SPOT Image, Inc */
#define TIFFTAG_GEOKEYDIRECTORY      34735
#define TIFFTAG_GEODOUBLEPARAMS      34736
#define TIFFTAG_GEOASCIIPARAMS       34737
#define FALSE 0
#define TRUE 1

TIFF *image = NULL;
unsigned int *buffer = NULL;
tsize_t stripSize = 0;
unsigned long bufferSize = 0;
int stripMax = 0;
unsigned int width = 0;
unsigned int height = 0;
unsigned int samplesperpixel = 0;
unsigned int bitspersample = 0;
unsigned int totalFrame = 0;
void *latlng;
void *scale;
unsigned short iCount;


static void TagExtender(TIFF *tiff)
{
	static const TIFFFieldInfo xtiffFieldInfo[] = {

		{ TIFFTAG_GEOPIXELSCALE,	-1,-1, TIFF_DOUBLE,	FIELD_CUSTOM,
		  TRUE,	TRUE,	"GeoPixelScale" },
		{ TIFFTAG_GEOTRANSMATRIX,	-1,-1, TIFF_DOUBLE,	FIELD_CUSTOM,
		  TRUE,	TRUE,	"GeoTransformationMatrix" },
		{ TIFFTAG_GEOTIEPOINTS,	-1,-1, TIFF_DOUBLE,	FIELD_CUSTOM,
		  TRUE,	TRUE,	"GeoTiePoints" },
		{ TIFFTAG_GEOKEYDIRECTORY, -1,-1, TIFF_SHORT,	FIELD_CUSTOM,
		  TRUE,	TRUE,	"GeoKeyDirectory" },
		{ TIFFTAG_GEODOUBLEPARAMS,	-1,-1, TIFF_DOUBLE,	FIELD_CUSTOM,
		  TRUE,	TRUE,	"GeoDoubleParams" },
		{ TIFFTAG_GEOASCIIPARAMS,	-1,-1, TIFF_ASCII,	FIELD_CUSTOM,
		  TRUE,	FALSE,	"GeoASCIIParams" }
	    };

	TIFFMergeFieldInfo( tiff, xtiffFieldInfo,
				sizeof(xtiffFieldInfo) / sizeof(xtiffFieldInfo[0]) );
}

/*
static
void _XTIFFInitialize(void)
{
    static int first_time=1;

    if (! first_time) return; //Been there. Done that.
    first_time = 0;

    /* Grab the inherited method and install 
    _ParentExtender = TIFFSetTagExtender(_XTIFFDefaultDirectory);
}

static void
_XTIFFDefaultDirectory(TIFF *tif)
{
    /* Install the extended Tag field info */
    //TIFFMergeFieldInfo(tif, xtiffFieldInfo, N(xtiffFieldInfo));

    /* Since an XTIFF client module may have overridden
     * the default directory method, we call it now to
     * allow it to set up the rest of its own methods.
    

    if (_ParentExtender)
        (*_ParentExtender)(tif);
}
*/
jint
Java_com_tiffdecoder_TiffDecoder_nativeTiffOpen( JNIEnv* env, jobject thiz, jstring path )
{
	// Open the TIFF image
	const char *strPath = NULL;
	strPath = (*env)->GetStringUTFChars(env, path, NULL );
    	TIFFSetTagExtender(TagExtender);

	if((image = TIFFOpen(strPath, "r")) == NULL){
		__android_log_print(ANDROID_LOG_INFO, "nativeTiffOpen", "Could not open incoming image", strPath);
    	return -1;
	}

	//TIFFMergeFieldInfo(image, xtiffFieldInfo, N(xtiffFieldInfo));

	(*env)->ReleaseStringUTFChars(env, path, strPath);

	// Read in the possibly multiple strips
	stripSize = TIFFStripSize (image);
	stripMax = TIFFNumberOfStrips (image);

	//bufferSize = stripMax * stripSize;

	totalFrame = TIFFNumberOfDirectories(image);
	TIFFGetField(image, TIFFTAG_IMAGEWIDTH, &width);
	TIFFGetField(image, TIFFTAG_IMAGELENGTH, &height);
	TIFFGetField(image, TIFFTAG_SAMPLESPERPIXEL, &samplesperpixel);
	TIFFGetField(image, TIFFTAG_BITSPERSAMPLE, &bitspersample);
	__android_log_print(ANDROID_LOG_INFO, "nativeTiffOpen", "before read long/lat");
	latlng = _TIFFmalloc(24);
    	TIFFGetField(image, TIFFTAG_GEOTIEPOINTS, &iCount, &latlng);
	__android_log_print(ANDROID_LOG_INFO, "nativeTiffOpen", "count=%d", iCount);
	__android_log_print(ANDROID_LOG_INFO, "nativeTiffOpen", "latlng0=%f", ((double *)latlng)[0]);
	__android_log_print(ANDROID_LOG_INFO, "nativeTiffOpen", "latlng1=%f", ((double *)latlng)[1]);
	__android_log_print(ANDROID_LOG_INFO, "nativeTiffOpen", "latlng2=%f", ((double *)latlng)[2]);
	__android_log_print(ANDROID_LOG_INFO, "nativeTiffOpen", "latlng3=%f", ((double *)latlng)[3]);
	__android_log_print(ANDROID_LOG_INFO, "nativeTiffOpen", "latlng4=%f", ((double *)latlng)[4]);
	__android_log_print(ANDROID_LOG_INFO, "nativeTiffOpen", "latlng5=%f", ((double *)latlng)[5]);
	scale = _TIFFmalloc(12);
	TIFFGetField(image, TIFFTAG_GEOPIXELSCALE, &iCount, &scale);


	__android_log_print(ANDROID_LOG_INFO, "nativeTiffOpen", "after read long/lat");
	bufferSize = width * height;
	// Allocate the memory
	if((buffer = (unsigned int *) _TIFFmalloc(bufferSize * sizeof (unsigned int))) == NULL){
		__android_log_print(ANDROID_LOG_INFO, "nativeTiffOpen", "Could not allocate enough memory for the uncompressed image");
		return -1;
	}

	return 0;
}

jintArray
Java_com_tiffdecoder_TiffDecoder_nativeTiffGetBytes( JNIEnv* env )
{
	int stripCount = 0;
	unsigned long imageOffset = 0;
	unsigned long result = 0;
	uint16 photo = 0;
	uint16 fillorder = 0;
	char tempbyte = 0;
	unsigned long count = 0;

	// Get the RGBA Image
	//TIFFReadRGBAImage(image, width, height, buffer, 0);
	TIFFReadRGBAImageOriented(image, width, height, buffer, ORIENTATION_TOPLEFT, 0);

	// Convert ABGR to ARGB
	int i = 0;
	int j = 0;
	int tmp = 0;
	for( i = 0; i < height; i++ )
	    for( j=0; j< width; j++ )
	    {
		tmp = buffer[ j + width * i ];
		buffer[ j + width * i ] = (tmp & 0xff000000) | ((tmp & 0x00ff0000)>>16) | (tmp & 0x0000ff00 ) | ((tmp & 0xff)<<16);
	    }
	// Deal with photometric interpretations
	if(TIFFGetField(image, TIFFTAG_PHOTOMETRIC, &photo) == 0){
			__android_log_print(ANDROID_LOG_INFO, "nativeTiffGetBytes", "Image has an undefined photometric interpretation");
		//;
	}
 
	/*
	if(photo != PHOTOMETRIC_MINISWHITE){
    // Flip bits
    	__android_log_print(ANDROID_LOG_INFO, "nativeTiffGetBytes", "Fixing the photometric interpretation");

    for(count = 0; count < bufferSize; count++)
      buffer[count] = ~buffer[count];
	}
	*/

	// Deal with fillorder
	if(TIFFGetField(image, TIFFTAG_FILLORDER, &fillorder) == 0){
    __android_log_print(ANDROID_LOG_INFO, "nativeTiffGetBytes", "Image has an undefined fillorder");
    //exit(42);
	}
	
	/*
	if(fillorder != FILLORDER_MSB2LSB){
    // We need to swap bits -- ABCDEFGH becomes HGFEDCBA
    __android_log_print(ANDROID_LOG_INFO, "nativeTiffGetBytes", "Fixing the fillorder");

	for(count = 0; count < bufferSize; count++){
		tempbyte = 0;
		if(buffer[count] & 128) tempbyte += 1;
		if(buffer[count] & 64) tempbyte += 2;
		if(buffer[count] & 32) tempbyte += 4;
		if(buffer[count] & 16) tempbyte += 8;
		if(buffer[count] & 8) tempbyte += 16;
		if(buffer[count] & 4) tempbyte += 32;
		if(buffer[count] & 2) tempbyte += 64;
		if(buffer[count] & 1) tempbyte += 128;
		buffer[count] = tempbyte;
	}
	}
	*/

	jintArray array = (*env)->NewIntArray( env, bufferSize );
	if (!array) {
			__android_log_print(ANDROID_LOG_INFO, "nativeTiffGetBytes", "OutOfMemoryError is thrown.");
	}else{
			jint* bytes = (*env)->GetIntArrayElements( env, array, NULL );
			if (bytes != NULL) {
				memcpy(bytes, buffer, bufferSize * sizeof (unsigned int));
				(*env)->ReleaseIntArrayElements( env, array, bytes, 0 );
			}
	}
	return array;
}


jfloatArray
Java_com_tiffdecoder_TiffDecoder_nativeTiffGetFloats( JNIEnv* env )
{
	__android_log_print(ANDROID_LOG_INFO, "MYPROG", "start of native code");
	
	__android_log_print(ANDROID_LOG_INFO, "MYPROG", "memory has been allocated");
	int row;	
	for(row=0; row<height; row++) {
		TIFFReadScanline(image, buffer+(row*width), row, 1);
	}
	
	jfloatArray array = (*env)->NewFloatArray( env, bufferSize );
	if (!array) {
			__android_log_print(ANDROID_LOG_INFO, "nativeTiffGetFloats", "OutOfMemoryError is thrown.");
	}
	else {

			jfloat* bytes = (*env)->GetFloatArrayElements( env, array, NULL );
			if (bytes != NULL) {
				memcpy(bytes, buffer, bufferSize * sizeof (float));
				//(*env)->ReleaseFloatArrayElements( env, array, bytes, 0 );
				(*env)->SetFloatArrayRegion(env, array, 0, bufferSize, bytes);
			}

    //memcpy(array, buffer, bufferSize * sizeof (float));

	}
	__android_log_print(ANDROID_LOG_INFO, "nativeTiffGetFloats", "Buffer size is %d", bufferSize);
	__android_log_print(ANDROID_LOG_INFO, "nativeTiffGetFloats", "width is %d", width);
	__android_log_print(ANDROID_LOG_INFO, "nativeTiffGetFloats", "height is %d", height);
	return array;
}

jfloat
Java_com_tiffdecoder_TiffDecoder_nativeTiffGetCornerLatitude( JNIEnv* env ) {
    return (float)((double *)latlng)[4];
}

jfloat
Java_com_tiffdecoder_TiffDecoder_nativeTiffGetCornerLongitude( JNIEnv* env ) {
    return (float)((double *)latlng)[3];
}

jfloat
Java_com_tiffdecoder_TiffDecoder_nativeTiffGetScale( JNIEnv* env ) {
    return (float)((double *)scale)[0];
}

jint
Java_com_tiffdecoder_TiffDecoder_nativeTiffGetLength( JNIEnv* env )
{
    return bufferSize;
}

jint
Java_com_tiffdecoder_TiffDecoder_nativeTiffGetWidth( JNIEnv* env )
{
    return width;
}

jint
Java_com_tiffdecoder_TiffDecoder_nativeTiffGetHeight( JNIEnv* env )
{
    return height;
}

void
Java_com_tiffdecoder_TiffDecoder_nativeTiffClose( JNIEnv* env )
{
    if(image)
    {
	TIFFClose(image);
	image = NULL;
    }
    if(buffer)
    {
	 _TIFFfree(buffer);
	buffer = NULL;
    }
}
