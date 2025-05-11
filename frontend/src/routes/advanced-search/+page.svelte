<script>
  import { onMount } from "svelte";
  import Post from "$lib/components/post.svelte"; // Import the individual Post component
  import Button from "$lib/components/ui/button/button.svelte"; // Import Button component
  import { PUBLIC_API_URL } from "$env/static/public"; // Import the public API URL

  let allPosts = []; // To store all posts fetched initially
  let filteredPosts = []; // To store posts matching the filters
  let filterOptions = {}; // To store unique values for each attribute
  let activeFilters = []; // To store the currently applied filters
  let selectedAttribute = ""; // Currently selected attribute in the dropdown
  let selectedValue = ""; // Currently selected value for the chosen attribute
  let minValue = ""; // For numerical range - minimum value
  let maxValue = ""; // For numerical range - maximum value
  let isLoading = true;
  let error = null;
  let availableAttributes = []; // To store attributes that have at least one non-null value

  const colorNameHexMap = {
    "Red": "#FF0000",
    "Green": "#00FF00",
    "Blue": "#0000FF",
    "Yellow": "#FFFF00",
    "Orange": "#FFA500",
    "Purple": "#800080",
    "Brown": "#A52A2A",
    "Black": "#000000",
    "White": "#FFFFFF",
    "Gray": "#808080"
  };

  const hexColorNameMap = Object.fromEntries(
    Object.entries(colorNameHexMap).map(([name, hex]) => [hex, name])
  );

  const filterableAttributes = [
    "material",
    "writtenText",
    "mainColor",
    "shape",
    "descriptionOfParts",
    "location",
    "hardness",
    "timePeriod",
    "smell",
    "taste",
    "texture",
    "value",
    "originOfAcquisition",
    "pattern",
    "brand",
    "print",
    "functionality",
    "imageLicenses",
    "markings",
    "handmade",
    "oneOfAKind",
    "width",
    "height",
    "length",
    "weight",
    "item_condition",
  ];

  const booleanAttributes = ["handmade", "oneOfAKind"];
  const numericalAttributes = ["weight", "value", "width", "height", "length"]; // Attributes that should use range selection
  const conditionEnumValues = ["NEW", "LIKE_NEW", "USED", "DAMAGED", "ANTIQUE"]; // From MysteryObject.java

  onMount(async () => {
    isLoading = true;
    error = null;
    try {
      // Fetch data from /api/posts/all/details/no-media using the imported get function
      allPosts = await fetch(
        `${PUBLIC_API_URL}/api/posts/all/details/no-media`,
      );
      allPosts = await allPosts.json();

      if (!Array.isArray(allPosts)) {
        console.error("Fetched data is not an array:", allPosts);
        throw new Error("Received invalid data format from server.");
      }

      extractFilterOptions();
      filteredPosts = [...allPosts]; // Initially show all posts
    } catch (err) {
      error = err.message;
      console.error("Error fetching posts:", err);
    } finally {
      isLoading = false;
    }
  });

  // Map between frontend attribute names and backend attribute names
  const attributeMapping = {
    width: "sizeX",
    height: "sizeY",
    length: "sizeZ",
  };

  // Get the backend attribute name for a given frontend attribute name
  function getBackendAttributeName(frontendAttr) {
    return attributeMapping[frontendAttr] || frontendAttr;
  }

  // Get the frontend attribute name for a given backend attribute name
  function getFrontendAttributeName(backendAttr) {
    for (const [frontendAttr, mappedBackendAttr] of Object.entries(
      attributeMapping,
    )) {
      if (mappedBackendAttr === backendAttr) {
        return frontendAttr;
      }
    }
    return backendAttr;
  }

  function extractFilterOptions() {
    const options = {};
    const attributesWithValues = new Set();

    filterableAttributes.forEach((attr) => {
      const values = new Set();
      let hasValues = false;

      // Use the backend attribute name when accessing post data
      const backendAttr = getBackendAttributeName(attr);

      allPosts.forEach((post) => {
        if (
          post.mysteryObject &&
          post.mysteryObject[backendAttr] !== null &&
          post.mysteryObject[backendAttr] !== undefined
        ) {
          values.add(post.mysteryObject[backendAttr]);
          hasValues = true;
        }
      });

      // Special handling for boolean and enum
      if (booleanAttributes.includes(attr)) {
        if (hasValues) {
          options[attr] = [true, false];
          attributesWithValues.add(attr);
        }
      } else if (attr === "item_condition") {
        if (hasValues) {
          options[attr] = conditionEnumValues;
          attributesWithValues.add(attr);
        }
      } else if (attr === "mainColor") {
        if (values.size > 0) {
          options[attr] = Array.from(values)
            .map(hex => ({ name: hexColorNameMap[hex] || hex, value: hex }))
            .sort((a, b) => a.name.localeCompare(b.name));
          attributesWithValues.add(attr);
        }
      } else {
        // For other attributes, only include if there are values
        if (values.size > 0) {
          options[attr] = Array.from(values).sort();
          attributesWithValues.add(attr);
        }
      }
    });

    // Update the available attributes list
    availableAttributes = filterableAttributes.filter((attr) =>
      attributesWithValues.has(attr),
    );
    filterOptions = options;
  }

  function addFilter() {
    // Avoid adding duplicate filters for the same attribute
    if (activeFilters.some((f) => f.attribute === selectedAttribute)) {
      console.warn(`Filter for ${selectedAttribute} already added.`);
      return;
    }

    // Handle numerical attributes with range selection
    if (numericalAttributes.includes(selectedAttribute)) {
      if (minValue !== "" || maxValue !== "") {
        // Convert to numbers for validation and storage
        const min =
          minValue === "" ? Number.MIN_SAFE_INTEGER : Number(minValue);
        const max =
          maxValue === "" ? Number.MAX_SAFE_INTEGER : Number(maxValue);

        // Validate that min <= max if both are provided
        if (
          minValue !== "" &&
          maxValue !== "" &&
          Number(minValue) > Number(maxValue)
        ) {
          console.warn("Min value must be less than or equal to max value");
          return;
        }

        activeFilters = [
          ...activeFilters,
          {
            attribute: selectedAttribute,
            type: "range",
            min,
            max,
            displayMin: minValue === "" ? "(any)" : minValue,
            displayMax: maxValue === "" ? "(any)" : maxValue,
          },
        ];

        // Reset selections
        minValue = "";
        maxValue = "";
      }
    }
    // Handle boolean attributes
    else if (
      booleanAttributes.includes(selectedAttribute) &&
      selectedValue !== ""
    ) {
      const valueToStore = selectedValue === "true";
      activeFilters = [
        ...activeFilters,
        { attribute: selectedAttribute, value: valueToStore },
      ];
      selectedValue = "";
    }
    // Handle other attributes
    else if (selectedValue !== "") {
      let filterToAdd = { attribute: selectedAttribute, value: selectedValue };
      if (selectedAttribute === "mainColor") {
        filterToAdd.displayName = hexColorNameMap[selectedValue] || selectedValue;
      }
      activeFilters = [
        ...activeFilters,
        filterToAdd,
      ];
      selectedValue = "";
    }
  }

  function removeFilter(attributeToRemove) {
    activeFilters = activeFilters.filter(
      (f) => f.attribute !== attributeToRemove,
    );
  }

  function performSearch() {
    if (activeFilters.length === 0) {
      filteredPosts = [...allPosts];
      return;
    }

    filteredPosts = allPosts.filter((post) => {
      if (!post.mysteryObject) return false; // Skip posts without mystery objects

      return activeFilters.every((filter) => {
        // Use the backend attribute name when accessing post data
        const backendAttr = getBackendAttributeName(filter.attribute);
        const postValue = post.mysteryObject[backendAttr];

        // Handle potential null/undefined values during comparison
        if (postValue === null || postValue === undefined) return false;

        // Handle range filters for numerical attributes
        if (filter.type === "range") {
          const numValue = Number(postValue);
          return numValue >= filter.min && numValue <= filter.max;
        }

        // Handle regular value filters (unchanged)
        return String(postValue) === String(filter.value);
      });
    });
  }

  // Reactive statements
  $: currentOptions = filterOptions[selectedAttribute] || [];
  $: if (selectedAttribute) {
    // Reset values when attribute changes
    if (
      !booleanAttributes.includes(selectedAttribute) &&
      selectedAttribute !== "item_condition"
    ) {
      selectedValue = "";
    }
    if (!numericalAttributes.includes(selectedAttribute)) {
      minValue = "";
      maxValue = "";
    }
  }
</script>

<svelte:head>
  <title>Advanced Search</title>
</svelte:head>

<div class="flex flex-col items-center bg-change dark:bg-dark shifting p-3 py-5">
  <div class="w-full max-w-7xl mx-auto">
    {#if isLoading}
      <div class="flex justify-center items-center py-16">
        <div
          class="inline-block h-10 w-10 border-4 border-neutral-200 dark:border-neutral-800 border-t-teal-600 dark:border-t-teal-500 rounded-full animate-spin"
        ></div>
      </div>
    {:else if error}
      <p class="text-red-500">Error: {error}</p>
    {:else}
      <!-- Filter Builder - Styling updated to match postContainer -->
      <div
        class="w-full bg-white dark:bg-neutral-950 shadow-sm rounded-md border border-neutral-200 dark:border-neutral-800 mb-4"
      >
        <div class="p-2.5 sm:p-3 border-b border-neutral-100 dark:border-neutral-800">
          <h2 class="flex items-center text-lg font-semibold text-neutral-900 dark:text-white gap-2">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-neutral-600 dark:text-neutral-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <span class="text-sm">Advanced Search</span>
          </h2>
        </div>
        
        <div class="p-3 sm:p-4">
          <div class="flex flex-wrap gap-3 items-end">
            <!-- Attribute Selection -->
            <div>
              <label
                for="attribute-select"
                class="block text-sm font-medium text-neutral-600 dark:text-neutral-400"
                >Attribute:</label
              >
              <select
                id="attribute-select"
                bind:value={selectedAttribute}
                class="mt-1 block w-full pl-3 pr-10 py-1.5 text-sm bg-white dark:bg-neutral-950 text-neutral-900 dark:text-white border border-neutral-200 dark:border-neutral-700 rounded-full shadow-sm focus:outline-none focus:ring-2 focus:ring-teal-500/30 focus:border-teal-500"
              >
                <option value="">-- Select Attribute --</option>
                {#each availableAttributes as attr}
                  <option value={attr}
                    >{attr
                      .replace(/([A-Z])/g, " $1")
                      .replace(/^./, (str) => str.toUpperCase())}</option
                  >
                  <!-- Add spaces before caps -->
                {/each}
              </select>
            </div>

            <!-- Value Selection -->
            {#if selectedAttribute}
              <!-- Numerical Range Selection -->
              {#if numericalAttributes.includes(selectedAttribute)}
                <div class="flex flex-col gap-2">
                  <label class="block text-sm font-medium text-neutral-600 dark:text-neutral-400"
                    >Range:</label
                  >
                  <div class="flex items-center gap-2">
                    <input
                      type="number"
                      bind:value={minValue}
                      placeholder="Min"
                      class="mt-1 block w-24 bg-white dark:bg-neutral-950 text-neutral-900 dark:text-white border border-neutral-200 dark:border-neutral-700 rounded-full shadow-sm focus:outline-none focus:ring-2 focus:ring-teal-500/30 focus:border-teal-500 text-xs py-1.5 px-3"
                    />
                    <span class="text-neutral-600 dark:text-neutral-400 text-xs">to</span>
                    <input
                      type="number"
                      bind:value={maxValue}
                      placeholder="Max"
                      class="mt-1 block w-24 bg-white dark:bg-neutral-950 text-neutral-900 dark:text-white border border-neutral-200 dark:border-neutral-700 rounded-full shadow-sm focus:outline-none focus:ring-2 focus:ring-teal-500/30 focus:border-teal-500 text-xs py-1.5 px-3"
                    />
                    <span class="text-xs text-neutral-500 dark:text-neutral-400">
                      {#if selectedAttribute === "weight"}
                        (grams)
                      {:else if selectedAttribute === "value"}
                        ($)
                      {:else if ["width", "height", "length"].includes(selectedAttribute)}
                        (cm)
                      {/if}
                    </span>
                  </div>
                  <p class="text-xs text-neutral-500 dark:text-neutral-400">
                    Leave empty for no limit
                  </p>
                </div>
                <!-- Boolean Selection -->
              {:else if booleanAttributes.includes(selectedAttribute)}
                <div>
                  <label
                    for="value-select"
                    class="block text-sm font-medium text-neutral-600 dark:text-neutral-400"
                    >Value:</label
                  >
                  <select
                    id="value-select"
                    bind:value={selectedValue}
                    class="mt-1 block w-full pl-3 pr-10 py-1.5 text-sm bg-white dark:bg-neutral-950 text-neutral-900 dark:text-white border border-neutral-200 dark:border-neutral-700 rounded-full shadow-sm focus:outline-none focus:ring-2 focus:ring-teal-500/30 focus:border-teal-500"
                  >
                    <option value="">-- Select Value --</option>
                    <option value="true">True</option>
                    <option value="false">False</option>
                  </select>
                </div>
                <!-- Condition Enum Selection -->
              {:else if selectedAttribute === "item_condition"}
                <div>
                  <label
                    for="value-select"
                    class="block text-sm font-medium text-neutral-600 dark:text-neutral-400"
                    >Value:</label
                  >
                  <select
                    id="value-select"
                    bind:value={selectedValue}
                    class="mt-1 block w-full pl-3 pr-10 py-1.5 text-sm bg-white dark:bg-neutral-950 text-neutral-900 dark:text-white border border-neutral-200 dark:border-neutral-700 rounded-full shadow-sm focus:outline-none focus:ring-2 focus:ring-teal-500/30 focus:border-teal-500"
                  >
                    <option value="">-- Select Condition --</option>
                    {#each currentOptions as option}
                      <option value={option}>{option}</option>
                    {/each}
                  </select>
                </div>
                <!-- Main Color Selection -->
              {:else if selectedAttribute === 'mainColor'}
                <div>
                  <label
                    for="value-select"
                    class="block text-sm font-medium text-neutral-600 dark:text-neutral-400"
                    >Value:</label
                  >
                  <select
                    id="value-select"
                    bind:value={selectedValue}
                    class="mt-1 block w-full pl-3 pr-10 py-1.5 text-sm bg-white dark:bg-neutral-950 text-neutral-900 dark:text-white border border-neutral-200 dark:border-neutral-700 rounded-full shadow-sm focus:outline-none focus:ring-2 focus:ring-teal-500/30 focus:border-teal-500"
                  >
                    <option value="">-- Select Color --</option>
                    {#each currentOptions as option (option.value)}
                      <option value={option.value}>{option.name}</option>
                    {/each}
                  </select>
                </div>
                <!-- Regular Selection with Options -->
              {:else if currentOptions.length > 0}
                <div>
                  <label
                    for="value-select"
                    class="block text-sm font-medium text-neutral-600 dark:text-neutral-400"
                    >Value:</label
                  >
                  <select
                    id="value-select"
                    bind:value={selectedValue}
                    class="mt-1 block w-full pl-3 pr-10 py-1.5 text-sm bg-white dark:bg-neutral-950 text-neutral-900 dark:text-white border border-neutral-200 dark:border-neutral-700 rounded-full shadow-sm focus:outline-none focus:ring-2 focus:ring-teal-500/30 focus:border-teal-500"
                  >
                    <option value="">-- Select Value --</option>
                    {#each currentOptions as option}
                      <option value={option}>{option}</option>
                    {/each}
                  </select>
                </div>
                <!-- Text Input for Other Cases -->
              {:else}
                <div>
                  <label
                    for="value-input"
                    class="block text-sm font-medium text-neutral-600 dark:text-neutral-400"
                    >Value:</label
                  >
                  <input
                    id="value-input"
                    type="text"
                    bind:value={selectedValue}
                    placeholder="Enter value"
                    class="mt-1 block w-full bg-white dark:bg-neutral-950 text-neutral-900 dark:text-white border border-neutral-200 dark:border-neutral-700 rounded-full shadow-sm focus:outline-none focus:ring-2 focus:ring-teal-500/30 focus:border-teal-500 text-xs py-1.5 px-3"
                  />
                </div>
              {/if}
            {/if}

            <!-- Add Filter Button -->
            <Button
              on:click={addFilter}
              disabled={!selectedAttribute ||
                (numericalAttributes.includes(selectedAttribute)
                  ? minValue === "" && maxValue === ""
                  : selectedValue === "")}
              class="text-xs bg-teal-600 hover:bg-teal-700 text-white px-2.5 py-0.5 rounded-full"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M10 5a1 1 0 011 1v3h3a1 1 0 110 2h-3v3a1 1 0 11-2 0v-3H6a1 1 0 110-2h3V6a1 1 0 011-1z" clip-rule="evenodd" />
              </svg>
              Add Filter
            </Button>
          </div>

          <!-- Active Filters Display -->
          {#if activeFilters.length > 0}
            <div class="mt-3 mb-2">
              <h3 class="text-sm font-medium text-neutral-600 dark:text-neutral-400 mb-1.5">
                Active Filters:
              </h3>
              <ul class="flex flex-wrap gap-1.5">
                {#each activeFilters as filter}
                  <li
                    class="bg-neutral-100 dark:bg-neutral-900 text-neutral-700 dark:text-neutral-300 rounded-full px-2.5 py-0.5 text-xs flex items-center border border-neutral-200 dark:border-neutral-700"
                  >
                    {#if filter.type === "range"}
                      {filter.attribute}: {filter.displayMin} to {filter.displayMax}
                      {#if filter.attribute === "weight"}
                        g
                      {:else if filter.attribute === "value"}
                        $
                      {:else if ["width", "height", "length"].includes(filter.attribute)}
                        cm
                      {/if}
                    {:else if filter.attribute === 'mainColor'}
                      {filter.attribute}: {filter.displayName || hexColorNameMap[filter.value] || String(filter.value)}
                    {:else}
                      {filter.attribute}: {String(filter.value)}
                    {/if}
                    <button
                      on:click={() => removeFilter(filter.attribute)}
                      class="ml-1 text-red-500 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300 font-semibold text-lg"
                      >×</button
                    >
                  </li>
                {/each}
              </ul>
            </div>
          {/if}
          
          <!-- Search Execution - Moved to left side -->
          <div class="mt-3 flex justify-start border-t border-neutral-100 dark:border-neutral-800 pt-2">
            <Button
              on:click={performSearch}
              class="text-xs bg-teal-600 hover:bg-teal-700 text-white px-2.5 py-0.5 rounded-full"
            >
              <svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 mr-1" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd" />
              </svg>
              Search Posts
            </Button>
          </div>
        </div>
      </div>

      <!-- Search Results with postContainer-like header -->
      <div class="search-results">
        <!-- Results header with styling matching postContainer -->
        <div class="w-full bg-white dark:bg-neutral-950 shadow-sm rounded-md border border-neutral-200 dark:border-neutral-800 mb-4">
          <div class="p-2.5 flex items-center justify-between">
            <div class="flex items-center gap-1.5">
              <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-neutral-600 dark:text-neutral-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
              </svg>
              <h3 class="font-medium text-sm text-neutral-800 dark:text-neutral-200">
                Results ({filteredPosts.length})
              </h3>
            </div>
          </div>
        </div>
        
        {#if filteredPosts.length > 0}
          <!-- Iterate and render individual Post components -->
          <div
            class="flex flex-col lg:flex-wrap lg:flex-row justify-center gap-4 lg:gap-6"
          >
            {#each filteredPosts as post (post.id)}
              <div class="w-full lg:w-[calc(33.333%-1rem)]">
                <a href={`/thread/${post.id}`} class="block">
                  <Post
                    id={post.id}
                    title={post.title}
                    description={post.description || ""}
                    tags={post.tags || []}
                    imageSrc={post.mysteryObject?.imageUrl || ""}
                    mediaFiles={post.mediaFiles || []}
                    postedBy={post.author}
                    createdAt={post.createdAt}
                    updatedAt={post.updatedAt}
                    upvotes={post.upvotes || 0}
                    downvotes={post.downvotes || 0}
                    commentCount={post.commentCount || 0}
                    userUpvoted={post.userUpvoted || false}
                    userDownvoted={post.userDownvoted || false}
                    solved={post.solved}
                    mysteryObject={post.mysteryObject || null}
                    variant="thumb"
                  />
                </a>
              </div>
            {/each}
          </div>
        {:else}
          <div class="bg-white dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800 p-6 text-center">
            <p class="text-neutral-600 dark:text-neutral-400">
              No posts match the current filters or no posts loaded.
            </p>
          </div>
        {/if}
      </div>
    {/if}
  </div>
</div>
