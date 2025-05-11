<script>
  import { getBadgeClasses } from '$lib/utils/badgeUtils';
  
  export let name = "";
  export let description = "";
  export let color = "slate";
  export let icon = "";
  
  const classes = getBadgeClasses(color);
</script>

<div class="group relative">
  <div 
    class="{classes.bg} {classes.text} {classes.border} flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium border transition hover:opacity-90 cursor-help"
  >
    <svg 
      xmlns="http://www.w3.org/2000/svg" 
      class="h-3.5 w-3.5 {classes.icon}" 
      viewBox="0 0 24 24" 
      fill="none" 
      stroke="currentColor" 
      stroke-width="2" 
      stroke-linecap="round" 
      stroke-linejoin="round"
    >
      <path d={icon}></path>
    </svg>
    <span>{name}</span>
  </div>
  
  <!-- Tooltip on hover -->
  <div class="opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-opacity absolute bottom-full mb-2 left-1/2 transform -translate-x-1/2 z-10 px-2 py-1.5 bg-neutral-800 text-white text-xs rounded shadow-lg whitespace-nowrap pointer-events-none">
    {description}
    <!-- Arrow -->
    <div class="absolute top-full left-1/2 transform -translate-x-1/2 border-4 border-transparent border-t-neutral-800"></div>
  </div>
</div>
