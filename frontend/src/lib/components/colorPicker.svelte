<script>
    import { createEventDispatcher } from 'svelte';
    
    export let value = '';
    export let hexValue = "#ffffff";
    let selectedHexColor = hexValue || "#ffffff";
    let isLoadingColorName = false;
    
    const dispatch = createEventDispatcher();
    
    // Function to ensure hex color is valid
    function sanitizeHexColor(hex) {
        // Ensure it starts with # and contains valid hex characters
        if (!hex) return "#ffffff";
        
        // Add # if missing
        if (!hex.startsWith('#')) {
            hex = '#' + hex;
        }
        
        // Validate the hex format
        if (!/^#([0-9A-Fa-f]{3}){1,2}$/.test(hex)) {
            console.warn("Invalid hex color, using default:", hex);
            return "#ffffff";
        }
        
        return hex;
    }
    
    // Function to fetch color name from TheColorAPI
    async function fetchColorName(hexColor) {
        isLoadingColorName = true;
        hexColor = sanitizeHexColor(hexColor);
        
        // Remove the # character from hex color
        const cleanHex = hexColor.replace('#', '');
        
        try {
            const response = await fetch(`https://www.thecolorapi.com/id?hex=${cleanHex}`);
            if (!response.ok) {
                throw new Error('Failed to fetch color name');
            }
            const data = await response.json();
            
            // Update color value with the name and keep hex
            value = data.name.value || 'Unknown color';
            hexValue = hexColor;
            dispatch('colorchange', { color: value, hex: hexColor });
        } catch (error) {
            console.error('Error fetching color name:', error);
            value = `Unknown color`;
            hexValue = hexColor;
            dispatch('colorchange', { color: value, hex: hexColor });
        } finally {
            isLoadingColorName = false;
        }
    }
    
    function handleColorChange(event) {
        selectedHexColor = sanitizeHexColor(event.target.value);
        fetchColorName(selectedHexColor);
    }
</script>

<div class="flex flex-col sm:flex-row sm:items-center gap-3">
    <input 
        type="color"
        id="color-picker"
        value={selectedHexColor}
        on:change={handleColorChange}
        class="w-full sm:w-14 h-10 border-none p-0 cursor-pointer rounded"
        aria-label="Select color"
    />
    <div class="flex items-center gap-2 flex-1 mt-2 sm:mt-0 border rounded p-2 bg-white dark:bg-neutral-800">
        <div 
            class="w-6 h-6 rounded border"
            style="background-color: {selectedHexColor};"
        ></div>
        <span class="text-sm truncate">
            {isLoadingColorName ? 'Loading...' : (value || 'Select a color')}
            <span class="text-xs opacity-75 ml-1">({selectedHexColor})</span>
        </span>
    </div>
</div>
