<script>
  import { onMount } from 'svelte';
  import { writable } from 'svelte/store';
  import { Button } from "$lib/components/ui/button";

  let searchTerm = '';
  let searchResults = writable([]); // Stores search results from Wikidata
  let selectedItems = writable([]); // Stores selected tags with format { id, label, description }
  let isLoading = writable(false); // Tracks loading state
  let showResults = writable(false); // Toggles search results dropdown visibility

  // Function to search Wikidata
  const searchWikidata = async () => {
    if (!searchTerm) {
      searchResults.set([]);
      showResults.set(false);
      return;
    }

    isLoading.set(true);
    showResults.set(true);

    try {
      const response = await fetch(`https://www.wikidata.org/w/api.php?action=wbsearchentities&search=${encodeURIComponent(searchTerm)}&language=en&format=json&origin=*`);
      const data = await response.json();
      // Map results to desired format
      const formattedResults = data.search.map(item => ({
        id: item.id,
        label: item.label,
        description: item.description || "No description available"
      }));
      searchResults.set(formattedResults);
    } catch (error) {
      console.error('Search failed', error);
      searchResults.set([]);
    } finally {
      isLoading.set(false);
    }
  };

  // Add a tag to the selected list
  const selectItem = (item) => {
    selectedItems.update((items) => {
      if (!items.find((i) => i.id === item.id)) {
        return [...items, item];
      }
      return items;
    });
    showResults.set(false);
  };

  // Remove a tag from the selected list
  const removeSelectedItem = (itemId) => {
    selectedItems.update((items) => items.filter((i) => i.id !== itemId));
  };

  // Hide search results dropdown when clicking outside
  onMount(() => {
    const handleClickOutside = (event) => {
      const searchContainer = document.querySelector('.search-container');
      if (!searchContainer.contains(event.target)) {
        showResults.set(false);
      }
    };
    document.addEventListener('click', handleClickOutside);
    return () => document.removeEventListener('click', handleClickOutside);
  });

  // Reflect changes in `selectedItems` to `tags` and 'labels'
  $: tags = $selectedItems;
  $: labels = $selectedItems;

  // Passed down to the parent component
  export let tags;
  export let labels; 

  // Function to get SVG icon for removal buttons
  function getRemovalIcon() {
    return `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>`;
  }
</script>

<div class="w-full mx-auto relative search-container text-sm">
  <!-- Search Input -->
  <input
    type="text"
    placeholder="Add relevant tags"
    bind:value={searchTerm}
    on:input={searchWikidata}
    class="w-full p-3 border border-gray-300 rounded-md bg-white dark:border-gray-600 dark:bg-neutral-950 text-sm text-black dark:text-white"
  />

  <!-- Loading Indicator -->
  {#if $isLoading}
    <p class="mt-2 text-black dark:text-white">Loading...</p>
  {/if}

  <!-- Search Results -->
  {#if $showResults && $searchResults.length > 0}
    <ul class="list-none p-0 mt-2 border bg-white dark:bg-neutral-950 rounded-md absolute w-full max-h-60 overflow-y-auto shadow-lg z-10">
      {#each $searchResults as result}
        <li class="mt-2">
          <button 
            on:click={() => selectItem(result)} 
            class="w-full text-left p-3 hover:bg-rose-900 hover:text-white text-black dark:text-white"
          >
            {result.label}: {result.description}
          </button>
        </li>
      {/each}
    </ul>
  {/if}

  <!-- Selected Items - Redesigned to match attribute cards -->
  <div class="selected-items mt-4">
    {#if $selectedItems.length > 0}
      <h3 class="text-md font-medium text-black dark:text-white mb-3 flex items-center">
        <span>Added Tags</span>
        <span class="ml-2 px-2 py-0.5 bg-gray-100 dark:bg-gray-800 text-xs rounded-full">
          {$selectedItems.length} added
        </span>
      </h3>
      
      <div class="grid grid-cols-1 gap-2 sm:grid-cols-2">
        {#each $selectedItems as selectedItem}
          <div class="relative p-3 rounded-md border bg-gray-50 dark:bg-neutral-900 dark:border-gray-700 transition-all hover:shadow-sm">
            <!-- Remove button in consistent style -->
            <button 
              type="button"
              class="absolute top-2 right-2 h-6 w-6 rounded-full flex items-center justify-center bg-gray-100 text-gray-500 hover:bg-red-50 hover:text-red-500 dark:bg-gray-800 dark:hover:bg-gray-700 transition-colors"
              on:click={() => removeSelectedItem(selectedItem.id)}
              title="Remove tag"
            >
              {@html getRemovalIcon()}
            </button>
            
            <!-- Tag content -->
            <div class="pr-6">
              <div class="font-medium text-sm mb-1">{selectedItem.label}</div>
              <a 
                href={`https://www.wikidata.org/wiki/${selectedItem.id}`} 
                target="_blank" 
                rel="noopener noreferrer"
                class="text-xs text-gray-500 hover:text-rose-700 dark:hover:text-rose-400 transition-colors"
              >
                {selectedItem.id}
              </a>
              {#if selectedItem.description}
                <p class="text-xs mt-1 line-clamp-2 text-gray-600 dark:text-gray-400">
                  {selectedItem.description}
                </p>
              {/if}
            </div>
          </div>
        {/each}
      </div>
    {:else}
      <p class="text-sm text-gray-500 dark:text-gray-400 mt-2">No tags added yet. Search for relevant terms above.</p>
    {/if}
  </div>
</div>
