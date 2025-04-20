<script>
  import { createEventDispatcher, onMount } from 'svelte';
  import { Input } from "$lib/components/ui/input";
  import { writable } from 'svelte/store';
  
  export let value = '';
  export let placeholder = 'Select or type a material';
  
  const dispatch = createEventDispatcher();
  
  // State
  let inputValue = value || '';
  let showSuggestions = false;
  let isLoading = writable(false);
  let materials = writable([]);
  let materialCategories = writable([]);
  let error = writable(null);
  
  // Helper function to capitalize the first letter
  function capitalizeFirstLetter(string) {
    if (!string) return '';
    return string.charAt(0).toUpperCase() + string.slice(1);
  }
  
  // Fetch materials from Wikidata
  async function searchMaterials(query) {
    if (!query || query.length < 2) {
      materials.set([]);
      return;
    }
    
    isLoading.set(true);
    error.set(null);
    
    try {
      // First try direct search for materials with Wikidata API
      const searchUrl = `https://www.wikidata.org/w/api.php?action=wbsearchentities&search=${encodeURIComponent(query)}&language=en&format=json&origin=*&limit=8&type=item`;
      
      const response = await fetch(searchUrl);
      if (!response.ok) throw new Error('Failed to fetch from Wikidata');
      
      const data = await response.json();
      
      // Filter results to include only materials (heuristic approach)
      // We'll enhance this by checking descriptions and other properties
      const results = data.search
        .filter(item => {
          const lowerLabel = item.label?.toLowerCase() || '';
          const lowerDesc = item.description?.toLowerCase() || '';
          
          // Material-related terms to look for in descriptions
          const materialTerms = [
            'material', 'substance', 'metal', 'alloy', 'polymer', 
            'ceramic', 'wood', 'plastic', 'fabric', 'textile',
            'compound', 'element', 'mineral'
          ];
          
          return materialTerms.some(term => lowerDesc.includes(term));
        })
        .map(item => ({
          id: item.id,
          label: item.label,
          description: item.description || 'No description available'
        }));
      
      materials.set(results);
    } catch (err) {
      console.error('Error fetching materials:', err);
      error.set('Failed to fetch materials. Please try again.');
      materials.set([]);
    } finally {
      isLoading.set(false);
    }
  }
  
  // Fetch material categories for initial display
  async function fetchMaterialCategories() {
    isLoading.set(true);
    error.set(null);
    
    try {
      // SPARQL query to get common material categories from Wikidata
      const sparqlQuery = `
        SELECT ?category ?categoryLabel WHERE {
          ?category wdt:P279 wd:Q214609. # subclass of material
          SERVICE wikibase:label { bd:serviceParam wikibase:language "[AUTO_LANGUAGE],en". }
        }
        LIMIT 15
      `;
      
      const url = `https://query.wikidata.org/sparql?query=${encodeURIComponent(sparqlQuery)}&format=json`;
      
      const response = await fetch(url);
      if (!response.ok) throw new Error('Failed to fetch material categories');
      
      const data = await response.json();
      
      const categories = data.results.bindings.map(binding => ({
        id: binding.category.value.split('/').pop(),
        label: binding.categoryLabel.value
      }));
      
      materialCategories.set(categories);
    } catch (err) {
      console.error('Error fetching material categories:', err);
      error.set('Failed to load material categories.');
      materialCategories.set([]);
    } finally {
      isLoading.set(false);
    }
  }
  
  // When input changes, trigger search with debounce
  let searchTimeout;
  function handleInputChange() {
    // Capitalize first letter of input
    if (inputValue) {
      inputValue = capitalizeFirstLetter(inputValue);
    }
    
    value = inputValue;
    dispatch('change', { value: inputValue });
    
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
      if (inputValue.length >= 2) {
        searchMaterials(inputValue);
      } else {
        materials.set([]);
      }
    }, 300);
  }
  
  function selectMaterial(material) {
    // Ensure the label is capitalized
    const capitalizedLabel = capitalizeFirstLetter(material.label);
    inputValue = capitalizedLabel;
    value = capitalizedLabel;
    
    dispatch('change', { 
      value: capitalizedLabel, 
      id: material.id,
      description: material.description 
    });
    showSuggestions = false;
  }
  
  // Function to directly select a category as the material
  function selectCategory(category) {
    const capitalizedLabel = capitalizeFirstLetter(category.label);
    inputValue = capitalizedLabel;
    value = capitalizedLabel;
    
    dispatch('change', { 
      value: capitalizedLabel, 
      id: category.id
    });
    showSuggestions = false;
  }
  
  function handleFocus() {
    showSuggestions = true;
    if (inputValue.length < 2 && $materialCategories.length === 0) {
      fetchMaterialCategories();
    } else if (inputValue.length >= 2) {
      searchMaterials(inputValue);
    }
  }
  
  // Hide dropdown when clicking outside
  onMount(() => {
    function handleClickOutside(event) {
      if (!event.target.closest('.material-selector')) {
        showSuggestions = false;
      }
    }
    
    document.addEventListener('click', handleClickOutside);
    
    return () => {
      document.removeEventListener('click', handleClickOutside);
    };
  });
</script>

<div class="material-selector relative">
  <Input 
    type="text"
    bind:value={inputValue}
    on:input={handleInputChange}
    on:focus={handleFocus}
    {placeholder}
    class="w-full rounded-full"
  />
  
  {#if showSuggestions}
    <div class="absolute z-20 mt-1 w-full max-h-60 overflow-y-auto bg-white dark:bg-neutral-900 shadow-lg border rounded-xl">
      <!-- Loading state -->
      {#if $isLoading}
        <div class="p-3 flex items-center justify-center">
          <div class="animate-spin h-4 w-4 border-2 border-neutral-300 rounded-full border-t-neutral-600 mr-2"></div>
          <span class="text-sm">Loading materials...</span>
        </div>
      <!-- Error state -->
      {:else if $error}
        <div class="p-3 text-sm text-red-500">{$error}</div>
      <!-- Empty state -->
      {:else if $materials.length === 0 && inputValue.length >= 2}
        <div class="p-3 text-sm text-neutral-500">No matching materials found. You can use your own term.</div>
      <!-- Search results -->
      {:else if inputValue.length >= 2 && $materials.length > 0}
        <div class="p-2">
          <div class="text-xs font-semibold mb-1 px-2 text-neutral-500">Material Suggestions</div>
          {#each $materials as material}
            <button 
              class="block w-full text-left px-3 py-2 text-sm hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-full transition-colors"
              on:click={() => selectMaterial(material)}>
              <div class="text-sm font-medium">{material.label}</div>
              {#if material.description}
                <div class="text-xs text-neutral-500 line-clamp-1">{material.description}</div>
              {/if}
            </button>
          {/each}
        </div>
      <!-- Material categories (shown when focused with empty or short input) -->
      {:else if $materialCategories.length > 0}
        <div class="p-2">
          <div class="text-xs font-semibold mb-1 px-2 text-neutral-500">Common Material Categories</div>
          {#each $materialCategories as category}
            <button 
              class="block w-full text-left px-3 py-2 text-sm hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-full transition-colors"
              on:click={() => selectCategory(category)}>
              <div class="text-sm font-medium">{category.label}</div>
              <div class="text-xs text-neutral-500">Select as material</div>
            </button>
          {/each}
          <div class="text-xs text-center mt-2 text-neutral-500">
            Type to search for specific materials
          </div>
        </div>
      {:else}
        <div class="p-3 text-sm text-neutral-500">Type at least 2 characters to search for materials</div>
      {/if}
    </div>
  {/if}
</div>
