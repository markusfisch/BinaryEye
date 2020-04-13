#!/usr/bin/env bash

# Try to find Inkscape or ImageMagick's convert
find_converter() {
	if [ -z "$INKSCAPE" ]
	then
		INKSCAPE=$(which inkscape) ||
			INKSCAPE='/Applications/Inkscape.app/Contents/MacOS/Inkscape'
	fi

	if [ -x "$INKSCAPE" ]
	then
		converter() {
			"$INKSCAPE" \
				"$PWD/$1" \
				-o "$PWD/$2" \
				-w "$3" \
				-h "$4"
		}
	elif which convert &>/dev/null
	then
		converter() {
			convert \
				-background none \
				"$1" \
				-thumbnail "${3}x${4}" \
				-strip \
				"$2"
		}
	else
		return 1
	fi
}

# Scale a length and cut off fraction
#
# @param 1 - length
# @param 2 - multiplier
scale() {
	echo "$1*$2" | bc -l | cut -d '.' -f 1
}

# Make sure $DIR exists
check_dir() {
	[ -d "$DIR" ] || mkdir -p "$DIR" || {
		echo "error: couldn't create $DIR" >&2
		return $?
	}
}

# Returns true if source is older than target file
#
# @param 1 - target file
# @param 2 - source file
newer_than() {
	[ -r "$1" ] && [ -z "$(find "$2" -type f -newer "$1")" ]
}

# Convert SVG files in multiple resolutions to PNG
#
# @param 1 - output path
update() {
	local SVG SIZE NEGATE
	while read -r SVG SIZE NEGATE
	do
		SIZE=${SIZE:-24}

		local DPI MULTIPLIER DIR PNG
		while read -r DPI MULTIPLIER
		do
			DIR="$1-$DPI"
			check_dir || return $?

			PNG=${SVG##*/}
			PNG="$DIR/${PNG%.*}.png"

			newer_than "$PNG" "$SVG" && continue

			converter \
				"$SVG" \
				"$PNG" \
				"$(scale "${SIZE%%x*}" "$MULTIPLIER")" \
				"$(scale "${SIZE##*x}" "$MULTIPLIER")"

			if (( NEGATE ))
			then
				convert "$PNG" -negate "$PNG"
			fi
		done <<EOF
xxxhdpi 4
xxhdpi 3
xhdpi 2
hdpi 1.5
mdpi 1
ldpi .75
EOF
	done
}

type converter &>/dev/null || find_converter || {
	echo "error: no Inkscape and no ImageMagick convert" >&2
	exit 1
}

update app/src/debug/res/mipmap << EOF
svg/debug/ic_launcher.svg 48
EOF

update app/src/main/res/mipmap << EOF
svg/ic_launcher.svg 48
EOF

update app/src/main/res/drawable << EOF
svg/ic_shortcut_decode.svg 48
svg/ic_shortcut_encode.svg 48
svg/ic_shortcut_preferences.svg 48
svg/logo.svg 80
svg/wallpaper.svg 384
EOF
