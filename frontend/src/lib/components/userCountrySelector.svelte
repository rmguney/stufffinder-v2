<script>
  import { onMount } from 'svelte';
  import { Button } from './ui/button';
  
  export let selectedCountries = [];
  export let isEditable = false;
  export let maxCountries = 3;
  
  let allCountries = [];
  let loading = true;
  let error = null;
  let isOpen = false;
  
  // Fetch countries from REST Countries API
  async function fetchCountries() {
    try {
      const response = await fetch('https://restcountries.com/v3.1/all?fields=name,flags,cca2');
      if (!response.ok) throw new Error('Failed to fetch countries');
      
      const data = await response.json();
      
      // Sort countries by name
      allCountries = data.sort((a, b) => 
        a.name.common.localeCompare(b.name.common)
      );
      
      loading = false;
    } catch (err) {
      console.error('Error fetching countries:', err);
      error = 'Could not load countries';
      loading = false;
    }
  }
  
  function addCountry(country) {
    if (selectedCountries.length < maxCountries && !selectedCountries.some(c => c.cca2 === country.cca2)) {
      selectedCountries = [...selectedCountries, country];
    }
    isOpen = false;
  }
  
  function removeCountry(countryCode) {
    selectedCountries = selectedCountries.filter(country => country.cca2 !== countryCode);
  }
  
  onMount(fetchCountries);
</script>

<div class="flex flex-col items-center w-full">
  <div class="flex flex-wrap gap-2 items-center justify-center">
    {#if selectedCountries.length > 0}
      {#each selectedCountries as country (country.cca2)}
        <div class="flex items-center gap-1 bg-white dark:bg-neutral-800 rounded-full pl-2 pr-1 py-1 shadow-sm border border-neutral-200 dark:border-neutral-700">
          <img src={country.flags.svg} alt="{country.name.common} flag" class="w-4 h-4 rounded-full object-cover" />
          <span class="text-xs">{country.name.common}</span>
          {#if isEditable}
            <button 
              class="text-neutral-500 hover:text-neutral-800 dark:text-neutral-400 dark:hover:text-neutral-200 p-1 rounded-full"
              on:click={() => removeCountry(country.cca2)}
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
            </button>
          {/if}
        </div>
      {/each}
    {:else}
      <span class="text-xs text-neutral-500 dark:text-neutral-400 italic text-center">No location provided</span>
    {/if}
    
    {#if isEditable && selectedCountries.length < maxCountries}
      <div class="relative">
        <Button 
          variant="ghost" 
          size="sm" 
          class="h-7 px-2 rounded-full" 
          on:click={() => isOpen = !isOpen}
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14"/><path d="M5 12h14"/></svg>
          <span class="sr-only">Add country</span>
        </Button>
        
        {#if isOpen}
          <div class="fixed inset-0 z-[999]" on:click|stopPropagation={() => isOpen = false}>
          </div>
          
          <!-- Improved dropdown - fixed z-index -->
          <div class="fixed z-[1000] mt-1 max-h-60 w-64 overflow-auto rounded-md bg-white dark:bg-neutral-950 text-foreground shadow-md border border-neutral-200 dark:border-neutral-700">
            {#if loading}
              <div class="p-2 text-center text-sm">Loading...</div>
            {:else if error}
              <div class="p-2 text-center text-sm text-destructive">{error}</div>
            {:else}
              <div class="p-1">
                {#each allCountries as country (country.cca2)}
                  <button
                    class="flex w-full items-center gap-2 rounded-sm px-2 py-1.5 text-sm outline-none hover:bg-accent hover:text-accent-foreground"
                    on:click|stopPropagation={() => addCountry(country)}
                  >
                    <img src={country.flags.svg} alt="{country.name.common} flag" class="w-4 h-4 rounded-full object-cover" />
                    <span>{country.name.common}</span>
                  </button>
                {/each}
              </div>
            {/if}
          </div>
        {/if}
      </div>
    {/if}
  </div>
</div>

<style>
  /* Add CSS to control z-index and overlay behavior */
  :global(.country-dropdown-overlay) {
    position: fixed;
    inset: 0;
    background: transparent;
    z-index: 999;
  }
  
  :global(.country-dropdown) {
    z-index: 1000;
    position: relative;
  }
</style>
