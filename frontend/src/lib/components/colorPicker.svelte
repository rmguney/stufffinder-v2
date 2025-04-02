<script>
    import { createEventDispatcher } from 'svelte';
    
    export let value = '';
    export let hexValue = "#ffffff";
    let selectedHexColor = hexValue || "#ffffff";
    let isLoadingColorName = false;
    let colorInputElement; // Reference to the color input
    
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
    
    // Function to open the color picker
    function openColorPicker() {
        if (colorInputElement) {
            colorInputElement.click();
        }
    }
</script>

<div class="flex items-center gap-3">
    <input 
        bind:this={colorInputElement}
        type="color"
        id="color-picker"
        value={selectedHexColor}
        on:change={handleColorChange}
        class="sr-only"
        aria-label="Select color"
    />
    <div 
        class="flex items-center gap-2 flex-1 border rounded p-2 bg-white dark:bg-neutral-800 cursor-pointer hover:bg-gray-50 dark:hover:bg-neutral-700"
        on:click={openColorPicker}
        on:keydown={(e) => e.key === 'Enter' && openColorPicker()}
        tabindex="0"
        role="button"
        aria-label="Open color picker"
    >
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
